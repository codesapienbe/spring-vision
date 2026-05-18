package io.github.codesapienbe.springvision.core.djl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.nn.norm.Dropout;
import ai.djl.nn.pooling.Pool;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.ParameterStore;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * In-process online damage classifier backed by a DJL PyTorch CNN.
 *
 * <p>Architecture: 3×Conv2d(BatchNorm, ReLU, MaxPool) → Flatten → Linear(256) →
 * Dropout(0.3) → Linear(22). Input: 3×128×128 RGB images.</p>
 *
 * <p>Training fires automatically when the in-memory buffer reaches
 * {@code minBatchSize}. Checkpoints are saved to {@code checkpointDir} and
 * restored on startup. Call {@link #exportModelArtifact(Path)} to copy the
 * checkpoint into a project models directory for JAR bundling.</p>
 */
@Component
public class DjlOnlineDamageClassifier {

    private static final Logger log = LoggerFactory.getLogger(DjlOnlineDamageClassifier.class);

    static final int INPUT_SIZE = 128;
    static final int NUM_CLASSES = 22;
    static final String MODEL_NAME = "damage-classifier";

    // Combined ordered taxonomy (indices 0-21)
    static final String[] ALL_DAMAGE_CLASSES;

    static {
        ALL_DAMAGE_CLASSES = new String[NUM_CLASSES];
        System.arraycopy(DjlVisionBackend.VEHICLE_DAMAGE_CLASSES, 0, ALL_DAMAGE_CLASSES, 0, 14);
        System.arraycopy(DjlVisionBackend.EXTENDED_DAMAGE_CLASSES, 0, ALL_DAMAGE_CLASSES, 14, 8);
    }

    private static final Map<String, Integer> CLASS_INDEX;

    static {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < ALL_DAMAGE_CLASSES.length; i++) {
            m.put(ALL_DAMAGE_CLASSES[i], i);
        }
        CLASS_INDEX = Collections.unmodifiableMap(m);
    }

    private final DjlProperties properties;
    private final ReentrantLock trainLock = new ReentrantLock();
    private final AtomicBoolean classifierReady = new AtomicBoolean(false);

    // In-memory buffer: raw float pixels [3*128*128] and class indices
    private final List<float[]> pixelBuffer = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> labelBuffer = Collections.synchronizedList(new ArrayList<>());

    private volatile Model model;

    public DjlOnlineDamageClassifier(DjlProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        model = Model.newInstance(MODEL_NAME, "PyTorch");
        model.setBlock(buildBlock());
        Path checkpoint = checkpointPath();
        if (Files.exists(checkpoint)) {
            try {
                model.load(checkpoint.getParent(), MODEL_NAME);
                classifierReady.set(true);
                log.info("Loaded damage classifier checkpoint from {}", checkpoint);
            } catch (Exception e) {
                log.warn("Could not load checkpoint {}: {} — starting fresh", checkpoint, e.getMessage());
            }
        } else {
            log.info("No damage classifier checkpoint found at {} — will train on first batch", checkpoint);
        }
    }

    @PreDestroy
    void shutdown() {
        if (model != null) {
            model.close();
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Adds a labeled sample to the buffer, persists it to disk in YOLO format,
     * and triggers training when the buffer reaches {@code minBatchSize}.
     *
     * @return current buffer size after adding the sample
     */
    public int addSample(ImageData imageData, String damageClass, BoundingBox bbox) {
        Integer classIdx = CLASS_INDEX.get(damageClass);
        if (classIdx == null) {
            throw new IllegalArgumentException("Unknown damage class: " + damageClass
                + ". Valid classes: " + String.join(", ", ALL_DAMAGE_CLASSES));
        }

        float[] pixels = resizeAndNormalize(imageData);
        persistToDisk(imageData, damageClass, classIdx, bbox);

        pixelBuffer.add(pixels);
        labelBuffer.add(classIdx);

        int size = pixelBuffer.size();
        int maxBuffer = properties.getOnlineClassifier().getMaxBufferSize();
        if (size > maxBuffer) {
            pixelBuffer.remove(0);
            labelBuffer.remove(0);
            size = pixelBuffer.size();
        }

        if (size >= properties.getOnlineClassifier().getMinBatchSize()) {
            trainStep();
        }
        return size;
    }

    /**
     * Runs the auxiliary classifier on the image. Returns {@code null} if not yet ready.
     */
    public Detection classify(ImageData imageData) {
        if (!classifierReady.get()) {
            return null;
        }
        try (NDManager manager = NDManager.newBaseManager("PyTorch")) {
            float[] pixels = resizeAndNormalize(imageData);
            NDArray input = manager.create(pixels, new Shape(1, 3, INPUT_SIZE, INPUT_SIZE));
            NDList output = model.getBlock()
                .forward(new ParameterStore(manager, false), new NDList(input), false);
            float[] logits = output.singletonOrThrow().toFloatArray();
            float[] probs = softmax(logits, NUM_CLASSES);
            int bestClass = argmax(probs);
            double confidence = probs[bestClass];
            Map<String, Object> attrs = new LinkedHashMap<>();
            attrs.put("source", "auxiliary-classifier");
            attrs.put("classIndex", bestClass);
            return new Detection(ALL_DAMAGE_CLASSES[bestClass], confidence, null, attrs);
        } catch (Exception e) {
            log.warn("Auxiliary classification failed: {}", e.getMessage());
            return null;
        }
    }

    /** Returns per-class sample counts from the on-disk dataset directory. */
    public Map<String, Long> getDatasetStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        Path labelsDir = datasetLabelsDir();
        if (!Files.exists(labelsDir)) {
            return stats;
        }
        try (Stream<Path> files = Files.list(labelsDir)) {
            files.filter(p -> p.toString().endsWith(".txt"))
                .forEach(p -> {
                    try {
                        Files.lines(p)
                            .filter(l -> !l.startsWith("#") && !l.isBlank())
                            .forEach(l -> {
                                String[] parts = l.trim().split("\\s+");
                                if (parts.length >= 1) {
                                    try {
                                        int idx = Integer.parseInt(parts[0]);
                                        if (idx >= 0 && idx < NUM_CLASSES) {
                                            stats.merge(ALL_DAMAGE_CLASSES[idx], 1L, Long::sum);
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            });
                    } catch (IOException e) {
                        log.debug("Could not read label file {}: {}", p, e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.warn("Could not list dataset labels: {}", e.getMessage());
        }
        return stats;
    }

    public boolean isReady() {
        return classifierReady.get();
    }

    /**
     * Copies the current checkpoint into {@code targetDir} for JAR bundling.
     * The checkpoint is saved under {@code targetDir/damage-classifier/}.
     */
    public void exportModelArtifact(Path targetDir) throws IOException {
        if (!classifierReady.get()) {
            throw new IllegalStateException("Classifier has not been trained yet");
        }
        Path dest = targetDir.resolve(MODEL_NAME);
        Files.createDirectories(dest);
        model.save(dest, MODEL_NAME);
        log.info("Exported damage classifier artifact to {}", dest);
    }

    // -----------------------------------------------------------------------
    // Training
    // -----------------------------------------------------------------------

    void trainStep() {
        if (!trainLock.tryLock()) {
            log.debug("Training already in progress — skipping this trigger");
            return;
        }
        try {
            List<float[]> pixelSnapshot;
            List<Integer> labelSnapshot;
            synchronized (pixelBuffer) {
                pixelSnapshot = new ArrayList<>(pixelBuffer);
                labelSnapshot = new ArrayList<>(labelBuffer);
            }
            int n = pixelSnapshot.size();
            if (n == 0) {
                return;
            }
            log.info("Starting DJL training step: {} samples, {} epochs",
                n, properties.getOnlineClassifier().getEpochs());

            try (NDManager manager = NDManager.newBaseManager("PyTorch")) {
                int pixelLen = 3 * INPUT_SIZE * INPUT_SIZE;
                float[] allPixels = new float[n * pixelLen];
                float[] allLabels = new float[n];
                for (int i = 0; i < n; i++) {
                    System.arraycopy(pixelSnapshot.get(i), 0, allPixels, i * pixelLen, pixelLen);
                    allLabels[i] = labelSnapshot.get(i);
                }
                NDArray features = manager.create(allPixels, new Shape(n, 3, INPUT_SIZE, INPUT_SIZE));
                NDArray labels = manager.create(allLabels, new Shape(n));

                int batchSize = Math.min(n, properties.getOnlineClassifier().getMinBatchSize());
                ArrayDataset dataset = new ArrayDataset.Builder()
                    .setData(features)
                    .optLabels(labels)
                    .setSampling(batchSize, true)
                    .build();

                DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                    .addEvaluator(new Accuracy())
                    .optOptimizer(Adam.builder()
                        .optLearningRateTracker(
                            Tracker.fixed((float) properties.getOnlineClassifier().getLearningRate()))
                        .build());

                try (Trainer trainer = model.newTrainer(config)) {
                    trainer.initialize(new Shape(batchSize, 3, INPUT_SIZE, INPUT_SIZE));
                    EasyTrain.fit(trainer, properties.getOnlineClassifier().getEpochs(), dataset, null);
                }
            }

            Path checkpointDir = checkpointPath().getParent();
            Files.createDirectories(checkpointDir);
            model.save(checkpointDir, MODEL_NAME);
            classifierReady.set(true);
            log.info("Training complete — checkpoint saved to {}", checkpointPath());
        } catch (Exception e) {
            log.error("DJL training step failed: {}", e.getMessage(), e);
        } finally {
            trainLock.unlock();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static SequentialBlock buildBlock() {
        return new SequentialBlock()
            .add(Conv2d.builder().setFilters(32).setKernelShape(new Shape(3, 3))
                .optPadding(new Shape(1, 1)).build())
            .add(BatchNorm.builder().build())
            .add(Activation.reluBlock())
            .add(Pool.maxPool2dBlock(new Shape(2, 2), new Shape(2, 2)))
            .add(Conv2d.builder().setFilters(64).setKernelShape(new Shape(3, 3))
                .optPadding(new Shape(1, 1)).build())
            .add(BatchNorm.builder().build())
            .add(Activation.reluBlock())
            .add(Pool.maxPool2dBlock(new Shape(2, 2), new Shape(2, 2)))
            .add(Conv2d.builder().setFilters(128).setKernelShape(new Shape(3, 3))
                .optPadding(new Shape(1, 1)).build())
            .add(BatchNorm.builder().build())
            .add(Activation.reluBlock())
            .add(Pool.maxPool2dBlock(new Shape(2, 2), new Shape(2, 2)))
            .add(Blocks.batchFlattenBlock())
            .add(Linear.builder().setUnits(256).build())
            .add(Activation.reluBlock())
            .add(Dropout.builder().optRate(0.3f).build())
            .add(Linear.builder().setUnits(NUM_CLASSES).build());
    }

    private float[] resizeAndNormalize(ImageData imageData) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = resized.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, INPUT_SIZE, INPUT_SIZE, null);
            g.dispose();

            // ImageNet-style normalization
            float[] mean = {0.485f, 0.456f, 0.406f};
            float[] std = {0.229f, 0.224f, 0.225f};
            float[] pixels = new float[3 * INPUT_SIZE * INPUT_SIZE];
            int[] rgbs = resized.getRGB(0, 0, INPUT_SIZE, INPUT_SIZE, null, 0, INPUT_SIZE);
            for (int i = 0; i < rgbs.length; i++) {
                int rgb = rgbs[i];
                pixels[i]                          = (((rgb >> 16) & 0xFF) / 255f - mean[0]) / std[0];
                pixels[INPUT_SIZE * INPUT_SIZE + i] = (((rgb >> 8) & 0xFF) / 255f - mean[1]) / std[1];
                pixels[2 * INPUT_SIZE * INPUT_SIZE + i] = ((rgb & 0xFF) / 255f - mean[2]) / std[2];
            }
            return pixels;
        } catch (IOException e) {
            throw new RuntimeException("Failed to resize/normalize image", e);
        }
    }

    private void persistToDisk(ImageData imageData, String damageClass, int classIdx, BoundingBox bbox) {
        try {
            Path imagesDir = datasetImagesDir();
            Path labelsDir = datasetLabelsDir();
            Files.createDirectories(imagesDir);
            Files.createDirectories(labelsDir);

            String stem = damageClass + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Path imgFile = imagesDir.resolve(stem + ".png");
            Path lblFile = labelsDir.resolve(stem + ".txt");

            Files.write(imgFile, imageData.data());

            double cx = bbox != null ? bbox.x() + bbox.width() / 2.0 : 0.5;
            double cy = bbox != null ? bbox.y() + bbox.height() / 2.0 : 0.5;
            double w  = bbox != null ? bbox.width()  : 1.0;
            double h  = bbox != null ? bbox.height() : 1.0;

            String label = String.format("%d %.6f %.6f %.6f %.6f%n", classIdx, cx, cy, w, h);
            Files.writeString(lblFile, "# class cx cy w h  (YOLO normalized)\n" + label);
            log.debug("Persisted training sample: {} → {}", damageClass, stem);
        } catch (IOException e) {
            log.warn("Could not persist training sample to disk: {}", e.getMessage());
        }
    }

    private Path checkpointPath() {
        String dir = properties.getOnlineClassifier().getCheckpointDir();
        String expanded = dir.replace("~", System.getProperty("user.home"));
        return Paths.get(expanded).resolve(MODEL_NAME + ".params");
    }

    private Path datasetImagesDir() {
        return datasetBaseDir().resolve("images/train");
    }

    private Path datasetLabelsDir() {
        return datasetBaseDir().resolve("labels/train");
    }

    private Path datasetBaseDir() {
        String dir = properties.getOnlineClassifier().getDatasetDir();
        String expanded = dir.replace("~", System.getProperty("user.home"));
        return Paths.get(expanded);
    }

    private static float[] softmax(float[] logits, int n) {
        float max = logits[0];
        for (int i = 1; i < n; i++) {
            if (logits[i] > max) max = logits[i];
        }
        float sum = 0f;
        float[] exp = new float[n];
        for (int i = 0; i < n; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < n; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    private static int argmax(float[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[best]) best = i;
        }
        return best;
    }
}
