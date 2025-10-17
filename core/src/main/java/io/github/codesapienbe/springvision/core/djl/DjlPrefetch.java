package io.github.codesapienbe.springvision.core.djl;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.modality.cv.output.CategoryMask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple DJL prefetch utility that loads common models to populate the DJL repository cache.
 * <p>
 * Usage: java -cp <classpath> io.github.codesapienbe.springvision.core.djl.DjlPrefetch [cacheDir]
 * If cacheDir is not provided it defaults to target/djl-cache.
 */
public class DjlPrefetch {

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        String cacheDir = args.length > 0 ? args[0] : "target/djl-cache";
        // Allow overriding engine via system property or environment variable for prefetch
        String engine = System.getProperty("spring.vision.djl.engine", System.getenv().getOrDefault("DJL_ENGINE", "PyTorch"));
        System.setProperty("ai.djl.repository.cache.dir", cacheDir);
        System.out.println("DJL prefetch: using cache dir = " + cacheDir + ", engine = " + engine);

        // New: allow build to continue when models can't be found unless strict mode enabled
        boolean strict = Boolean.parseBoolean(System.getProperty("spring.vision.djl.prefetch.strict", System.getenv().getOrDefault("SPRING_VISION_DJL_PREFETCH_STRICT", "false")));
        if (strict) {
            System.out.println("DJL prefetch running in strict mode: missing required models will fail the process");
        } else {
            System.out.println("DJL prefetch running in non-strict mode: missing required models will be tolerated (build will continue)");
        }

        try {
            // Ensure cache dir exists
            Files.createDirectories(Path.of(cacheDir));

            // Load a set of models similar to what the runtime will use. Loading triggers downloads
            // of model artifacts and native engine libraries into the cache dir.
            // Required models: if these cannot be resolved we must fail the build (unless non-strict)
            try {
                ensureRequiredFaceDetection(cacheDir, engine);
            } catch (Exception e) {
                System.err.println("Required face detection prefetch failed: " + e.getMessage());
                if (strict) throw e;
                else System.err.println("Continuing despite missing required face detection model (non-strict mode)");
            }

            try {
                ensureRequiredPoseModel(cacheDir, engine);
            } catch (Exception e) {
                System.err.println("Required pose model prefetch failed: " + e.getMessage());
                if (strict) throw e;
                else System.err.println("Continuing despite missing required pose model (non-strict mode)");
            }

            try {
                ensureRequiredActionModel(cacheDir, engine);
            } catch (Exception e) {
                System.err.println("Required action recognition prefetch failed: " + e.getMessage());
                if (strict) throw e;
                else
                    System.err.println("Continuing despite missing required action recognition model (non-strict mode)");
            }

            // Non-fatal models (prefetch best-effort)
            safeLoad(() -> loadObjectDetectionModel(engine), "object detection");
            safeLoad(DjlPrefetch::loadSegmentationModels, "segmentation");
            safeLoad(DjlPrefetch::loadFaceRecognitionModel, "face recognition/classification");

            // Final verification: check required artifacts exist in cache(s)
            try {
                verifyRequiredArtifactsPresent(cacheDir);
            } catch (Exception e) {
                System.err.println("Verification of required artifacts failed: " + e.getMessage());
                if (strict) throw e;
                else System.err.println("Verification failed but continuing in non-strict mode");
            }

            System.out.println("DJL prefetch completed successfully");
        } catch (Exception e) {
            System.err.println("DJL prefetch failed: " + e.getMessage());
            e.printStackTrace(System.err);
            // Fail the build by exiting non-zero so CI can detect missing artifacts only when strict
            if (Boolean.parseBoolean(System.getProperty("spring.vision.djl.prefetch.strict", System.getenv().getOrDefault("SPRING_VISION_DJL_PREFETCH_STRICT", "false")))) {
                System.exit(2);
            } else {
                // In non-strict mode, do not kill the build; log and continue
                System.err.println("Non-strict prefetch failure; allowing build to continue.");
            }
        }
    }

    // Check whether any candidate name exists in the cache directory tree
    private static boolean isCandidatePresentInCache(String cacheDir, String[] candidates) {
        try {
            // Check provided cacheDir
            Path root = Path.of(cacheDir);
            if (Files.exists(root)) {
                try (var stream = Files.walk(root)) {
                    boolean found = stream
                        .map(Path::toString)
                        .anyMatch(p -> {
                            for (String c : candidates) {
                                if (p.contains(c)) return true;
                            }
                            return false;
                        });
                    if (found) return true;
                }
            }

            // Also check the user's default DJL cache (if available via DjlModelLoader)
            try {
                Path defaultCache = DjlModelLoader.getCacheDirectory();
                if (Files.exists(defaultCache)) {
                    try (var stream = Files.walk(defaultCache)) {
                        boolean found = stream
                            .map(Path::toString)
                            .anyMatch(p -> {
                                for (String c : candidates) {
                                    if (p.contains(c)) return true;
                                }
                                return false;
                            });
                        if (found) return true;
                    }
                }
            } catch (Throwable ignore) {
                // if DjlModelLoader not accessible or any IO error occurs, ignore and continue
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static void ensureRequiredFaceDetection(String cacheDir, String engine) {
        try {
            String[] faceCandidates = new String[]{"retinaface"};
            if (isCandidatePresentInCache(cacheDir, faceCandidates)) {
                System.out.println("Found retinaface in cache; skipping remote load.");
                return;
            }
            Criteria.Builder<Image, DetectedObjects> builder = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optEngine(engine);
            try (ZooModel<Image, DetectedObjects> m = tryLoadWithCandidates(builder, Device.cpu(), faceCandidates)) {
                if (m.getName().toLowerCase().contains("retinaface")) {
                    System.out.println("Required retinaface model loaded: " + m.getName());
                } else {
                    throw new IllegalStateException("Retinaface model not found or loaded: " + m.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: required retinaface model could not be loaded or found in cache: " + e.getMessage());
            throw new IllegalStateException("Failed to ensure retinaface model", e);
        }
    }

    private static void ensureRequiredPoseModel(String cacheDir, String engine) {
        try {
            String[] poseCandidates = new String[]{"simple_pose", "movenet", "openpose", "simple_pose_hrnet"};
            if (isCandidatePresentInCache(cacheDir, poseCandidates)) {
                System.out.println("Found pose estimation model candidate in cache; skipping remote load.");
                return;
            }
            Criteria.Builder<Image, Joints> builder = Criteria.builder()
                .optApplication(Application.CV.POSE_ESTIMATION)
                .setTypes(Image.class, Joints.class)
                .optEngine(engine);
            try (ZooModel<Image, Joints> m = tryLoadWithCandidates(builder, Device.cpu(), poseCandidates)) {
                System.out.println("Required pose estimation model loaded: " + m.getName());
            }
        } catch (Exception e) {
            System.err.println("ERROR: required pose estimation model could not be loaded or found in cache: " + e.getMessage());
            throw new IllegalStateException("Failed to ensure pose estimation model", e);
        }
    }

    private static void ensureRequiredActionModel(String cacheDir, String engine) {
        try {
            String[] actionCandidates = new String[]{"action_recognition", "r2plus1d_18", "Human-Action-Recognition-VIT-Base-patch16-224", "timesformer", "i3d"};
            if (isCandidatePresentInCache(cacheDir, actionCandidates)) {
                System.out.println("Found action recognition model candidate in cache; skipping remote load.");
                return;
            }
            Criteria.Builder<Image, float[]> builder = Criteria.builder()
                .optApplication(Application.CV.ACTION_RECOGNITION)
                .setTypes(Image.class, float[].class)
                .optEngine(engine);
            try (ZooModel<Image, float[]> m = tryLoadWithCandidates(builder, Device.cpu(), actionCandidates)) {
                System.out.println("Required action recognition model loaded: " + m.getName());
            }
        } catch (Exception e) {
            System.err.println("ERROR: required action recognition model could not be loaded or found in cache: " + e.getMessage());
            throw new IllegalStateException("Failed to ensure action recognition model", e);
        }
    }

    // Helper that tries candidate model filters for a given Criteria builder before falling back
    private static <I, O> ZooModel<I, O> tryLoadWithCandidates(Criteria.Builder<I, O> builder, Device device, String[] candidates)
        throws ModelNotFoundException, MalformedModelException, IOException {
        for (String candidate : candidates) {
            try {
                Criteria<I, O> c = builder.optFilter("model", candidate).optDevice(device).optProgress(new ProgressBar()).build();
                ZooModel<I, O> m = c.loadModel();
                System.out.println("Loaded model for candidate '" + candidate + "': " + m.getName());
                return m;
            } catch (ModelNotFoundException mnf) {
                System.err.println("Model not found for candidate '" + candidate + "': " + mnf.getMessage());
            } catch (Exception ex) {
                System.err.println("Failed loading candidate '" + candidate + "': " + ex.getMessage());
            }
        }
        // Fallback permissive criteria: just request the application type and device
        try {
            Criteria<I, O> permissive = builder.optDevice(device).optProgress(new ProgressBar()).build();
            ZooModel<I, O> m = permissive.loadModel();
            System.out.println("Loaded model via permissive criteria: " + m.getName());
            return m;
        } catch (ModelNotFoundException mnf) {
            System.err.println("No model found via permissive criteria: " + mnf.getMessage());
            throw mnf;
        }
    }

    private static void safeLoad(ThrowingRunnable loader, String name) {
        try {
            loader.run();
        } catch (ModelNotFoundException e) {
            System.err.println("Model not found for " + name + ": " + e.getMessage());
        } catch (MalformedModelException e) {
            System.err.println("Malformed model for " + name + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error while prefetching " + name + ": " + e.getMessage());
        } catch (Exception t) {
            System.err.println("Unexpected error while prefetching " + name + ": " + t.getMessage());
        }
    }

    private static void loadObjectDetectionModel(String engine) throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching object detection model...");
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, DetectedObjects.class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .optEngine(engine)
            .build();
        try (ZooModel<Image, DetectedObjects> m = criteria.loadModel()) {
            System.out.println("Loaded object detection: " + m.getName());
        }
    }

    private static void loadSegmentationModels() throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching segmentation models...");
        Criteria<Image, CategoryMask> semanticCriteria = Criteria.builder()
            .optApplication(Application.CV.SEMANTIC_SEGMENTATION)
            .setTypes(Image.class, CategoryMask.class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, CategoryMask> m = semanticCriteria.loadModel()) {
            System.out.println("Loaded semantic segmentation: " + m.getName());
        }

        Criteria<Image, DetectedObjects> instanceCriteria = Criteria.builder()
            .optApplication(Application.CV.INSTANCE_SEGMENTATION)
            .setTypes(Image.class, DetectedObjects.class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, DetectedObjects> m = instanceCriteria.loadModel()) {
            System.out.println("Loaded instance segmentation: " + m.getName());
        }
    }

    private static void loadFaceRecognitionModel() throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching face recognition/model (image classification)...");
        Criteria<Image, float[]> criteria = Criteria.builder()
            .optApplication(Application.CV.IMAGE_CLASSIFICATION)
            .setTypes(Image.class, float[].class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, float[]> m = criteria.loadModel()) {
            System.out.println("Loaded face recognition/classification: " + m.getName());
        }
    }

    private static void verifyRequiredArtifactsPresent(String cacheDir) {
        // Define required model tokens
        String[] faceReq = new String[]{"retinaface"};
        String[] poseReq = new String[]{"simple_pose", "movenet", "openpose", "simple_pose_hrnet"};
        String[] actionReq = new String[]{"action_recognition", "r2plus1d_18", "Human-Action-Recognition-VIT-Base-patch16-224", "timesformer", "i3d"};

        boolean faceOk = isCandidatePresentInCache(cacheDir, faceReq);
        boolean poseOk = isCandidatePresentInCache(cacheDir, poseReq);
        boolean actionOk = isCandidatePresentInCache(cacheDir, actionReq);

        if (!faceOk || !poseOk || !actionOk) {
            System.err.println("ERROR: Required models missing in cache after prefetch:");
            if (!faceOk) System.err.println(" - retinaface (required)");
            if (!poseOk) System.err.println(" - pose estimation model (one of: " + String.join(", ", poseReq) + ")");
            if (!actionOk)
                System.err.println(" - action recognition model (one of: " + String.join(", ", actionReq) + ")");
            throw new IllegalStateException("Required DJL models not available in cache");
        }
        System.out.println("Verification: all required model artifacts are present in cache(s)");
    }

}
