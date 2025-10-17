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
        System.setProperty("ai.djl.repository.cache.dir", cacheDir);
        System.out.println("DJL prefetch: using cache dir = " + cacheDir);

        try {
            // Ensure cache dir exists
            Files.createDirectories(Path.of(cacheDir));

            // Load a set of models similar to what the runtime will use. Loading triggers downloads
            // of model artifacts and native engine libraries into the cache dir.
            safeLoad(DjlPrefetch::loadObjectDetectionModel, "object detection");
            safeLoad(DjlPrefetch::loadPoseEstimationModel, "pose estimation");
            safeLoad(DjlPrefetch::loadActionRecognitionModel, "action recognition");
            safeLoad(DjlPrefetch::loadSegmentationModels, "segmentation");
            safeLoad(DjlPrefetch::loadFaceRecognitionModel, "face recognition/classification");

            System.out.println("DJL prefetch completed successfully");
        } catch (Exception e) {
            System.err.println("DJL prefetch failed: " + e.getMessage());
            e.printStackTrace(System.err);
            // Fail the build by exiting non-zero so CI can detect missing artifacts.
            // If there was a non-model related error (like an IO error creating cache dirs), fail.
            System.exit(2);
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

    private static void loadObjectDetectionModel() throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching object detection model...");
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, DetectedObjects.class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, DetectedObjects> m = criteria.loadModel()) {
            System.out.println("Loaded object detection: " + m.getName());
        }
    }

    private static void loadPoseEstimationModel() throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching pose estimation model...");
        Criteria<Image, Joints> criteria = Criteria.builder()
            .optApplication(Application.CV.POSE_ESTIMATION)
            .setTypes(Image.class, Joints.class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, Joints> m = criteria.loadModel()) {
            System.out.println("Loaded pose estimation: " + m.getName());
        }
    }

    private static void loadActionRecognitionModel() throws IOException, ModelNotFoundException, MalformedModelException {
        System.out.println("Prefetching action recognition model...");
        Criteria<Image, float[]> criteria = Criteria.builder()
            .optApplication(Application.CV.ACTION_RECOGNITION)
            .setTypes(Image.class, float[].class)
            .optDevice(Device.cpu())
            .optProgress(new ProgressBar())
            .build();
        try (ZooModel<Image, float[]> m = criteria.loadModel()) {
            System.out.println("Loaded action recognition: " + m.getName());
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
}
