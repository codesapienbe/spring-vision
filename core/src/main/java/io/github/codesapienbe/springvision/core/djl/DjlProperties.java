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

    /**
     * Vehicle damage detection settings
     */
    private VehicleDamage vehicleDamage = new VehicleDamage();

    /**
     * Online damage classifier training settings
     */
    private OnlineClassifier onlineClassifier = new OnlineClassifier();

    /**
     * Where model files come from when the build did not bundle them, and when they are loaded
     */
    private Models models = new Models();
    // Nested configuration classes
    public static class FaceDetection {
        private float confidenceThreshold = 0.7f;
        private int maxFaces = 100;

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
        private String backbone = "resnet50";
        private float confidenceThreshold = 0.5f;
        private int topK = 10;

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
        private int joints = 17;
        private float confidenceThreshold = 0.5f;

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

    /**
     * {@code spring.vision.djl.models.*}. The defaults keep the library and Docker behaviour: models
     * come from the classpath and are all loaded at start-up.
     */
    public static class Models {
        /** Download models that are not on the classpath into {@code directory}, in the background. */
        private boolean download = false;
        /** Where downloaded models are kept. A leading {@code ~} is the user's home. */
        private String directory = "~/.springvision/models";
        /** Load each model on its first use instead of at start-up. */
        private boolean lazy = false;

        public boolean isDownload() { return download; }
        public void setDownload(boolean download) { this.download = download; }

        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }

        public boolean isLazy() { return lazy; }
        public void setLazy(boolean lazy) { this.lazy = lazy; }
    }

    public static class OnlineClassifier {
        private boolean enabled = true;
        private int minBatchSize = 8;
        private int maxBufferSize = 500;
        private int epochs = 5;
        private double learningRate = 0.001;
        private String checkpointDir = "~/.springvision/";
        private String datasetDir = "datasets/vehicle-damage";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMinBatchSize() { return minBatchSize; }
        public void setMinBatchSize(int minBatchSize) { this.minBatchSize = minBatchSize; }

        public int getMaxBufferSize() { return maxBufferSize; }
        public void setMaxBufferSize(int maxBufferSize) { this.maxBufferSize = maxBufferSize; }

        public int getEpochs() { return epochs; }
        public void setEpochs(int epochs) { this.epochs = epochs; }

        public double getLearningRate() { return learningRate; }
        public void setLearningRate(double learningRate) { this.learningRate = learningRate; }

        public String getCheckpointDir() { return checkpointDir; }
        public void setCheckpointDir(String checkpointDir) { this.checkpointDir = checkpointDir; }

        public String getDatasetDir() { return datasetDir; }
        public void setDatasetDir(String datasetDir) { this.datasetDir = datasetDir; }
    }

    public static class VehicleDamage {
        private double minConfidence = 0.25;
        private double reliableConfidenceThreshold = 0.60;
        private float iouThreshold = 0.45f;

        public double getMinConfidence() {
            return minConfidence;
        }

        public void setMinConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
        }

        public double getReliableConfidenceThreshold() {
            return reliableConfidenceThreshold;
        }

        public void setReliableConfidenceThreshold(double reliableConfidenceThreshold) {
            this.reliableConfidenceThreshold = reliableConfidenceThreshold;
        }

        public float getIouThreshold() {
            return iouThreshold;
        }

        public void setIouThreshold(float iouThreshold) {
            this.iouThreshold = iouThreshold;
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

    public VehicleDamage getVehicleDamage() {
        return vehicleDamage;
    }

    public void setVehicleDamage(VehicleDamage vehicleDamage) {
        this.vehicleDamage = vehicleDamage;
    }

    public OnlineClassifier getOnlineClassifier() {
        return onlineClassifier;
    }

    public void setOnlineClassifier(OnlineClassifier onlineClassifier) {
        this.onlineClassifier = onlineClassifier;
    }

    public Models getModels() {
        return models;
    }

    public void setModels(Models models) {
        this.models = models;
    }

}
