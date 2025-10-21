package io.github.codesapienbe.springvision.core.djl;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;
import ai.djl.translate.TranslateException;

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

    private static final String CLASSPATH_PREFIX = "classpath:/models/";

    /**
     * Creates Criteria for YOLOv8 object detection models.
     *
     * @param modelSize Model size: "n" (nano), "s" (small), "m" (medium), "l" (large), "x" (extra large)
     * @return Criteria configured for object detection
     */
    public static Criteria<Image, DetectedObjects> createDetectionCriteria(String modelSize) {
        return Criteria.builder()
            .setTypes(Image.class, DetectedObjects.class)
            .optModelUrls(CLASSPATH_PREFIX + "yolov8/yolov8" + modelSize + ".pt")
            .optEngine("PyTorch")
            .optOption("mapLocation", "true")  // Load to CPU
            .build();
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
        return Criteria.builder()
            .setTypes(Image.class, Image.class)
            .optModelUrls(CLASSPATH_PREFIX + "yolov8-seg/yolov8" + modelSize + "-seg.pt")
            .optEngine("PyTorch")
            .build();
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
        return Criteria.builder()
            .setTypes(Image.class, Joints.class)
            .optModelUrls(CLASSPATH_PREFIX + "yolov8-pose/yolov8" + modelSize + "-pose.pt")
            .optEngine("PyTorch")
            .build();
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
        return Criteria.builder()
            .setTypes(Image.class, ai.djl.modality.Classifications.class)
            .optModelUrls(CLASSPATH_PREFIX + "yolov8-cls/yolov8n-cls.pt")
            .optEngine("PyTorch")
            .build();
    }

    /**
     * Creates Criteria for YOLOv8 oriented bounding box models.
     *
     * @return Criteria configured for oriented bounding box detection using yolov8n-obb.pt
     */
    public static Criteria<Image, DetectedObjects> createObbCriteria() {
        return Criteria.builder()
            .setTypes(Image.class, DetectedObjects.class)
            .optModelUrls(CLASSPATH_PREFIX + "yolov8-obb/yolov8n-obb.pt")
            .optEngine("PyTorch")
            .optOption("mapLocation", "true")  // Load to CPU
            .build();
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
        var resource = YoloLoader.class.getClassLoader().getResource("models/" + modelPath);
        return resource != null ? CLASSPATH_PREFIX + modelPath : null;
    }
}
