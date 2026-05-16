package io.github.codesapienbe.springvision.core.djl;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for DJL vision backend.
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
@ConfigurationProperties(prefix = "spring.vision.djl")
public class DjlProperties {

    /**
     * Whether DJL backend is enabled.
     */
    private boolean enabled = true;

    /**
     * Preferred engine: pytorch (default), mxnet, tensorflow, onnx
     */
    private String engine = "PyTorch";

    /**
     * Device: cpu (default), gpu, gpu:0, gpu:1, etc.
     */
    private String device = "cpu";

    /**
     * Model cache directory. Defaults to ~/.djl.ai
     */
    private String modelCacheDir;

    /**
     * Auto-download models from ModelZoo
     */
    private boolean autoDownload = true;

    /**
     * Enable progress bar during model download
     */
    private boolean showProgress = false;

    /**
     * Model zoo location (comma-separated URLs).
     */
    private String modelZooLocation;

    /**
     * Maximum number of concurrent inference threads.
     */
    private int maxConcurrentInferences = 4;

    /**
     * Face detection settings
     */
    private FaceDetection faceDetection = new FaceDetection();

    /**
     * Object detection settings
     */
    private ObjectDetection objectDetection = new ObjectDetection();

    /**
     * Pose estimation settings
     */
    private PoseEstimation poseEstimation = new PoseEstimation();

    /**
     * Action recognition settings
     */
    private ActionRecognition actionRecognition = new ActionRecognition();

    /**
     * Segmentation settings
     */
    private Segmentation segmentation = new Segmentation();

    /**
     * Face recognition settings
     */
    private FaceRecognition faceRecognition = new FaceRecognition();
    // Nested configuration classes
    public static class FaceDetection {
        private String model = "retinaface"; // or "lightface"
        private float confidenceThreshold = 0.7f;
        private int maxFaces = 100;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public int getMaxFaces() {
            return maxFaces;
        }

        public void setMaxFaces(int maxFaces) {
            this.maxFaces = maxFaces;
        }
    }

    public static class ObjectDetection {
        private String model = "ssd"; // or "yolo" (yolo models need proper TorchScript format)
        private String backbone = "resnet50";
        private float confidenceThreshold = 0.5f;
        private int topK = 10;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBackbone() {
            return backbone;
        }

        public void setBackbone(String backbone) {
            this.backbone = backbone;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class PoseEstimation {
        private String model = "yolo";
        private int joints = 17;
        private float confidenceThreshold = 0.5f;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getJoints() {
            return joints;
        }

        public void setJoints(int joints) {
            this.joints = joints;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    public static class ActionRecognition {
        private String model = "action_recognition";
        private float confidenceThreshold = 0.6f;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    public static class Segmentation {
        private String model = "instance_segmentation"; // or "semantic_segmentation"
        private boolean instanceLevel = true;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isInstanceLevel() {
            return instanceLevel;
        }

        public void setInstanceLevel(boolean instanceLevel) {
            this.instanceLevel = instanceLevel;
        }
    }

    public static class FaceRecognition {
        private String model = "face_feature"; // DJL FaceFeatureNet (PyTorch, 112x112, 512-dim)
        private int embeddingSize = 512;
        private float similarityThreshold = 0.6f;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getEmbeddingSize() {
            return embeddingSize;
        }

        public void setEmbeddingSize(int embeddingSize) {
            this.embeddingSize = embeddingSize;
        }

        public float getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(float similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getModelCacheDir() {
        return modelCacheDir;
    }

    public void setModelCacheDir(String modelCacheDir) {
        this.modelCacheDir = modelCacheDir;
    }

    /**
     * Convenience method - alias for getModelCacheDir().
     *
     * @return the model cache directory
     */
    public String getCacheDir() {
        return modelCacheDir;
    }

    public boolean isAutoDownload() {
        return autoDownload;
    }

    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
    }

    public String getModelZooLocation() {
        return modelZooLocation;
    }

    public void setModelZooLocation(String modelZooLocation) {
        this.modelZooLocation = modelZooLocation;
    }

    public int getMaxConcurrentInferences() {
        return maxConcurrentInferences;
    }

    public void setMaxConcurrentInferences(int maxConcurrentInferences) {
        this.maxConcurrentInferences = maxConcurrentInferences;
    }

    public FaceDetection getFaceDetection() {
        return faceDetection;
    }

    public void setFaceDetection(FaceDetection faceDetection) {
        this.faceDetection = faceDetection;
    }

    public ObjectDetection getObjectDetection() {
        return objectDetection;
    }

    public void setObjectDetection(ObjectDetection objectDetection) {
        this.objectDetection = objectDetection;
    }

    public PoseEstimation getPoseEstimation() {
        return poseEstimation;
    }

    public void setPoseEstimation(PoseEstimation poseEstimation) {
        this.poseEstimation = poseEstimation;
    }

    public ActionRecognition getActionRecognition() {
        return actionRecognition;
    }

    public void setActionRecognition(ActionRecognition actionRecognition) {
        this.actionRecognition = actionRecognition;
    }

    public Segmentation getSegmentation() {
        return segmentation;
    }

    public void setSegmentation(Segmentation segmentation) {
        this.segmentation = segmentation;
    }

    public FaceRecognition getFaceRecognition() {
        return faceRecognition;
    }

    public void setFaceRecognition(FaceRecognition faceRecognition) {
        this.faceRecognition = faceRecognition;
    }

}
