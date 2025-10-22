package io.github.codesapienbe.springvision.core.djl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;
import io.github.codesapienbe.springvision.core.djl.translator.YoloDetectionTranslator;

/**
 * Utility class for loading YOLOv8 models with pre-configured Criteria.
 * This class provides factory methods for different YOLOv8 model types:
 * <ul>
 *   <li>Object Detection (yolov8n.pt, yolov8s.pt, yolov8m.pt, yolov8l.pt, yolov8x.pt)</li>
 *   <li>Segmentation (yolov8n-seg.pt, yolov8s-seg.pt, yolov8m-seg.pt)</li>
 *   <li>Pose Estimation (yolov8n-pose.pt, yolov8s-pose.pt, yolov8m-pose.pt)</li>
 *   <li>Classification (yolov8n-cls.pt)</li>
 *   <li>Oriented Bounding Box (yolov8n-obb.pt)</li>
 * </ul>
 *
 * <p>Models are loaded from the classpath under /models/yolov8* directories.
 * Use the download-models Maven profile to download models during build time.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Criteria<Image, DetectedObjects> criteria = YoloLoader.createDetectionCriteria("yolov8n");
 * Predictor<Image, DetectedObjects> predictor = criteria.loadModel().newPredictor();
 * }</pre>
 */
public class YoloLoader {

    private static final String CLASSPATH_PREFIX = "models/";

    // Cache for extracted temporary model files to avoid re-extraction
    private static final Map<String, Path> tempModelCache = new ConcurrentHashMap<>();

    /**
     * Extracts a classpath resource to a temporary file and returns its file:// URL.
     * Uses caching to avoid re-extraction of the same model.
     *
     * @param modelPath Relative path to the model file (e.g., "yolov8/yolov8n.pt")
     * @return file:// URL to the extracted temporary file
     * @throws IOException if extraction fails
     */
    private static String extractToTempFile(String modelPath) throws IOException {
        return tempModelCache.computeIfAbsent(modelPath, path -> {
            try {
                var resource = YoloLoader.class.getClassLoader().getResource(CLASSPATH_PREFIX + path);
                if (resource == null) {
                    throw new IOException("Model not found in classpath: " + CLASSPATH_PREFIX + path);
                }

                // Create temporary file with same name
                var tempFile = Files.createTempFile("spring-vision-", "-" + Path.of(path).getFileName().toString());

                // Extract resource to temporary file
                try (InputStream input = resource.openStream()) {
                    Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                return tempFile;
            } catch (IOException e) {
                throw new RuntimeException("Failed to extract model to temporary file: " + path, e);
            }
        }).toUri().toString();
    }

    /**
     * Creates Criteria for YOLOv8 object detection models.
     *
     * @param modelSize Model size: "n" (nano), "s" (small), "m" (medium), "l" (large), "x" (extra large)
     * @return Criteria configured for object detection
     */
    public static Criteria<Image, DetectedObjects> createDetectionCriteria(String modelSize) {
        try {
            String modelPath = "yolov8/yolov8" + modelSize + ".pt";
            String modelUrl = extractToTempFile(modelPath);
            return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optOption("mapLocation", "true")  // Load to CPU
                .optTranslator(new YoloDetectionTranslator())
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLOv8 detection model: " + modelSize, e);
        }
    }

    /**
     * Creates Criteria for YOLOv8 object detection models with default nano model.
     *
     * @return Criteria configured for object detection using yolov8n.pt
     */
    public static Criteria<Image, DetectedObjects> createDetectionCriteria() {
        return createDetectionCriteria("n");
    }

    /**
     * Creates Criteria for YOLOv8 segmentation models.
     *
     * @param modelSize Model size: "n" (nano), "s" (small), "m" (medium)
     * @return Criteria configured for image segmentation
     */
    public static Criteria<Image, Image> createSegmentationCriteria(String modelSize) {
        try {
            String modelPath = "yolov8-seg/yolov8" + modelSize + "-seg.pt";
            String modelUrl = extractToTempFile(modelPath);
            return Criteria.builder()
                .setTypes(Image.class, Image.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLOv8 segmentation model: " + modelSize, e);
        }
    }

    /**
     * Creates Criteria for YOLOv8 segmentation models with default nano model.
     *
     * @return Criteria configured for image segmentation using yolov8n-seg.pt
     */
    public static Criteria<Image, Image> createSegmentationCriteria() {
        return createSegmentationCriteria("n");
    }

    /**
     * Creates Criteria for YOLOv8 pose estimation models.
     *
     * @param modelSize Model size: "n" (nano), "s" (small), "m" (medium)
     * @return Criteria configured for pose estimation
     */
    public static Criteria<Image, Joints> createPoseCriteria(String modelSize) {
        try {
            String modelPath = "yolov8-pose/yolov8" + modelSize + "-pose.pt";
            String modelUrl = extractToTempFile(modelPath);
            return Criteria.builder()
                .setTypes(Image.class, Joints.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLOv8 pose model: " + modelSize, e);
        }
    }

    /**
     * Creates Criteria for YOLOv8 pose estimation models with default nano model.
     *
     * @return Criteria configured for pose estimation using yolov8n-pose.pt
     */
    public static Criteria<Image, Joints> createPoseCriteria() {
        return createPoseCriteria("n");
    }

    /**
     * Creates Criteria for YOLOv8 classification models.
     *
     * @return Criteria configured for image classification using yolov8n-cls.pt
     */
    public static Criteria<Image, ai.djl.modality.Classifications> createClassificationCriteria() {
        try {
            String modelPath = "yolov8-cls/yolov8n-cls.pt";
            String modelUrl = extractToTempFile(modelPath);
            return Criteria.builder()
                .setTypes(Image.class, ai.djl.modality.Classifications.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLOv8 classification model", e);
        }
    }

    /**
     * Creates Criteria for YOLOv8 oriented bounding box models.
     *
     * @return Criteria configured for oriented bounding box detection using yolov8n-obb.pt
     */
    public static Criteria<Image, DetectedObjects> createObbCriteria() {
        try {
            String modelPath = "yolov8-obb/yolov8n-obb.pt";
            String modelUrl = extractToTempFile(modelPath);
            return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optOption("mapLocation", "true")  // Load to CPU
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YOLOv8 OBB model", e);
        }
    }

    /**
     * Validates that a model file exists in the classpath.
     *
     * @param modelPath Relative path to the model file (e.g., "yolov8/yolov8n.pt")
     * @return true if the model file exists in classpath, false otherwise
     */
    public static boolean isModelAvailable(String modelPath) {
        try {
            return YoloLoader.class.getClassLoader().getResource("models/" + modelPath) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the full classpath URL for a model file.
     *
     * @param modelPath Relative path to the model file (e.g., "yolov8/yolov8n.pt")
     * @return Full classpath URL or null if not found
     */
    public static String getModelUrl(String modelPath) {
        try {
            return extractToTempFile(modelPath);
        } catch (IOException e) {
            return null;
        }
    }
}
