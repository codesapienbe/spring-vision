package io.github.codesapienbe.springvision.facebytes.config;

import io.github.codesapienbe.springvision.facebytes.enums.DetectorBackend;
import io.github.codesapienbe.springvision.facebytes.enums.DistanceMetric;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;

/**
 * Configuration class for DeepFace/FaceBytes models and settings.
 * Provides centralized configuration management through environment variables and system properties.
 */
public final class DeepFaceConfig {

    private static final String COSINE_ENV = "FACEBYTES_THRESHOLD_COSINE";
    private static final String EUCLIDEAN_ENV = "FACEBYTES_THRESHOLD_EUCLIDEAN";
    private static final String EUCLIDEAN_L2_ENV = "FACEBYTES_THRESHOLD_EUCLIDEAN_L2";

    private static final String COSINE_SYS = "facebytes.threshold.cosine";
    private static final String EUCLIDEAN_SYS = "facebytes.threshold.euclidean";
    private static final String EUCLIDEAN_L2_SYS = "facebytes.threshold.euclidean_l2";

    // Per-model threshold overrides (ArcFace, VGGFace)
    private static final String ARCFACE_COS_ENV = "FACEBYTES_ARCFACE_THRESHOLD_COSINE";
    private static final String ARCFACE_EUC_ENV = "FACEBYTES_ARCFACE_THRESHOLD_EUCLIDEAN";
    private static final String ARCFACE_EUC_L2_ENV = "FACEBYTES_ARCFACE_THRESHOLD_EUCLIDEAN_L2";
    private static final String ARCFACE_COS_SYS = "facebytes.arcface.threshold.cosine";
    private static final String ARCFACE_EUC_SYS = "facebytes.arcface.threshold.euclidean";
    private static final String ARCFACE_EUC_L2_SYS = "facebytes.arcface.threshold.euclidean_l2";

    private static final String VGG_COS_ENV = "FACEBYTES_VGGFACE_THRESHOLD_COSINE";
    private static final String VGG_EUC_ENV = "FACEBYTES_VGGFACE_THRESHOLD_EUCLIDEAN";
    private static final String VGG_EUC_L2_ENV = "FACEBYTES_VGGFACE_THRESHOLD_EUCLIDEAN_L2";
    private static final String VGG_COS_SYS = "facebytes.vggface.threshold.cosine";
    private static final String VGG_EUC_SYS = "facebytes.vggface.threshold.euclidean";
    private static final String VGG_EUC_L2_SYS = "facebytes.vggface.threshold.euclidean_l2";

    private static final String INPUT_SIZE_ENV = "FACEBYTES_INPUT_SIZE";
    private static final String INPUT_SIZE_SYS = "facebytes.input.size";

    private static final String ALIGN_ENV = "FACEBYTES_ALIGN";
    private static final String ALIGN_SYS = "facebytes.align";

    private static final String ENFORCE_ENV = "FACEBYTES_ENFORCE_DETECTION";
    private static final String ENFORCE_SYS = "facebytes.enforce_detection";

    private static final String DETECTOR_ENV = "FACEBYTES_DETECTOR";
    private static final String DETECTOR_SYS = "facebytes.detector";

    private static final String MARGIN_ENV = "FACEBYTES_MARGIN";
    private static final String MARGIN_SYS = "facebytes.margin";

    private static final String DISTANCE_ENV = "FACEBYTES_DISTANCE";
    private static final String DISTANCE_SYS = "facebytes.distance";

    private static final String EMOTION_ENV = "FACEBYTES_EMOTION_ONNX_PATH";
    private static final String EMOTION_SYS = "facebytes.emotion.onnx";
    private static final String EMOTION_SIZE_ENV = "FACEBYTES_EMOTION_SIZE";
    private static final String EMOTION_SIZE_SYS = "facebytes.emotion.size";

    private static final String GENDER_ENV = "FACEBYTES_GENDER_ONNX_PATH";
    private static final String GENDER_SYS = "facebytes.gender.onnx";
    private static final String GENDER_SIZE_ENV = "FACEBYTES_GENDER_SIZE";
    private static final String GENDER_SIZE_SYS = "facebytes.gender.size";

    private static final String AGE_ENV = "FACEBYTES_AGE_ONNX_PATH";
    private static final String AGE_SYS = "facebytes.age.onnx";
    private static final String AGE_SIZE_ENV = "FACEBYTES_AGE_SIZE";
    private static final String AGE_SIZE_SYS = "facebytes.age.size";

    private static final String RACE_ENV = "FACEBYTES_RACE_ONNX_PATH";
    private static final String RACE_SYS = "facebytes.race.onnx";
    private static final String RACE_SIZE_ENV = "FACEBYTES_RACE_SIZE";
    private static final String RACE_SIZE_SYS = "facebytes.race.size";

    private static final String VGG_ENV = "FACEBYTES_VGGFACE_ONNX_PATH";
    private static final String VGG_SYS = "facebytes.vggface.onnx";

    // Auto-download and network settings for model management
    private static final String AUTO_DOWNLOAD_ENV = "FACEBYTES_AUTO_DOWNLOAD";
    private static final String AUTO_DOWNLOAD_SYS = "facebytes.auto_download";
    private static final String DOWNLOAD_CONNECT_TIMEOUT_ENV = "FACEBYTES_DOWNLOAD_CONNECT_TIMEOUT_MS";
    private static final String DOWNLOAD_CONNECT_TIMEOUT_SYS = "facebytes.download.connect_timeout_ms";
    private static final String DOWNLOAD_READ_TIMEOUT_ENV = "FACEBYTES_DOWNLOAD_READ_TIMEOUT_MS";
    private static final String DOWNLOAD_READ_TIMEOUT_SYS = "facebytes.download.read_timeout_ms";
    private static final String DOWNLOAD_ALLOW_INSECURE_ENV = "FACEBYTES_DOWNLOAD_ALLOW_INSECURE";
    private static final String DOWNLOAD_ALLOW_INSECURE_SYS = "facebytes.download.allow_insecure";

    private static final String ARCFACE_ENV = "FACEBYTES_ARCFACE_ONNX_PATH";
    private static final String ARCFACE_SYS = "facebytes.arcface.onnx";
    private static final String ARCFACE_SIZE_ENV = "FACEBYTES_ARCFACE_SIZE";
    private static final String ARCFACE_SIZE_SYS = "facebytes.arcface.size";

    private static final String FACENET_ENV = "FACEBYTES_FACENET_ONNX_PATH";
    private static final String FACENET_SYS = "facebytes.facenet.onnx";
    private static final String FACENET_SIZE_ENV = "FACEBYTES_FACENET_SIZE";
    private static final String FACENET_SIZE_SYS = "facebytes.facenet.size";

    private static final String FACENET512_ENV = "FACEBYTES_FACENET512_ONNX_PATH";
    private static final String FACENET512_SYS = "facebytes.facenet512.onnx";
    private static final String FACENET512_SIZE_ENV = "FACEBYTES_FACENET512_SIZE";
    private static final String FACENET512_SIZE_SYS = "facebytes.facenet512.size";

    private static final String OPENFACE_ENV = "FACEBYTES_OPENFACE_ONNX_PATH";
    private static final String OPENFACE_SYS = "facebytes.openface.onnx";
    private static final String OPENFACE_SIZE_ENV = "FACEBYTES_OPENFACE_SIZE";
    private static final String OPENFACE_SIZE_SYS = "facebytes.openface.size";

    private static final String SFACE_ENV = "FACEBYTES_SFACE_ONNX_PATH";
    private static final String SFACE_SYS = "facebytes.sface.onnx";
    private static final String SFACE_SIZE_ENV = "FACEBYTES_SFACE_SIZE";
    private static final String SFACE_SIZE_SYS = "facebytes.sface.size";

    private static final String DEEPFACE_ENV = "FACEBYTES_DEEPFACE_ONNX_PATH";
    private static final String DEEPFACE_SYS = "facebytes.deepface.onnx";
    private static final String DEEPFACE_SIZE_ENV = "FACEBYTES_DEEPFACE_SIZE";
    private static final String DEEPFACE_SIZE_SYS = "facebytes.deepface.size";

    // Global ONNX enable/disable switch (env/sys)
    private static final String ONNX_ENABLED_ENV = "VISION_MODEL_ONNX_ENABLED";
    private static final String ONNX_ENABLED_SYS = "vision.model.onnx.enabled";

    // RetinaFace detection model configuration
    private static final String RETINAFACE_ENV = "FACEBYTES_RETINAFACE_ONNX_PATH";
    private static final String RETINAFACE_SYS = "facebytes.retinaface.onnx";
    private static final String RETINAFACE_SIZE_ENV = "FACEBYTES_RETINAFACE_SIZE";
    private static final String RETINAFACE_SIZE_SYS = "facebytes.retinaface.size";
    private static final String RETINAFACE_SCORE_ENV = "FACEBYTES_RETINAFACE_SCORE_THR";
    private static final String RETINAFACE_SCORE_SYS = "facebytes.retinaface.score";
    private static final String RETINAFACE_NMS_ENV = "FACEBYTES_RETINAFACE_NMS_THR";
    private static final String RETINAFACE_NMS_SYS = "facebytes.retinaface.nms";

    // Normalization and color-space configuration
    private static final String NORM_MEAN_ENV = "FACEBYTES_NORM_MEAN";
    private static final String NORM_MEAN_SYS = "facebytes.norm.mean";
    private static final String NORM_STD_ENV = "FACEBYTES_NORM_STD";
    private static final String NORM_STD_SYS = "facebytes.norm.std";
    private static final String COLORSPACE_ENV = "FACEBYTES_COLOR_SPACE";
    private static final String COLORSPACE_SYS = "facebytes.color.space";

    // Landmark template override per model (comma-separated x1,y1,x2,y2,x3,y3 for base 112px template)
    private static final String ARCFACE_TEMPLATE_ENV = "FACEBYTES_TEMPLATE_ARCFACE";
    private static final String ARCFACE_TEMPLATE_SYS = "facebytes.template.arcface";
    private static final String SFACE_TEMPLATE_ENV = "FACEBYTES_TEMPLATE_SFACE";
    private static final String SFACE_TEMPLATE_SYS = "facebytes.template.sface";
    private static final String VGG_TEMPLATE_ENV = "FACEBYTES_TEMPLATE_VGG";
    private static final String VGG_TEMPLATE_SYS = "facebytes.template.vgg";

    private final double cosineThreshold;
    private final double euclideanThreshold;
    private final double euclideanL2Threshold;
    private final int inputSize;
    private final boolean align;
    private final boolean enforceDetection;
    private final DetectorBackend detectorBackend;
    private final int margin;

    private DeepFaceConfig(double cosineThreshold, double euclideanThreshold, double euclideanL2Threshold,
                           int inputSize, boolean align, DetectorBackend detectorBackend, int margin) {
        this.cosineThreshold = cosineThreshold;
        this.euclideanThreshold = euclideanThreshold;
        this.euclideanL2Threshold = euclideanL2Threshold;
        this.inputSize = inputSize;
        this.align = align;
        this.enforceDetection = readBoolean(ENFORCE_ENV, ENFORCE_SYS, true);
        this.detectorBackend = detectorBackend;
        this.margin = Math.max(0, margin);
    }

    /**
     * Gets the current configuration.
     *
     * @return The current configuration.
     */
    public static DeepFaceConfig current() {
        double cos = readDouble(COSINE_ENV, COSINE_SYS, 0.68);
        double euc = readDouble(EUCLIDEAN_ENV, EUCLIDEAN_SYS, 0.60);
        double eucL2 = readDouble(EUCLIDEAN_L2_ENV, EUCLIDEAN_L2_SYS, 1.13);
        int size = (int) readDouble(INPUT_SIZE_ENV, INPUT_SIZE_SYS, 224);
        boolean align = readBoolean(ALIGN_ENV, ALIGN_SYS, true);
        DetectorBackend backend = readDetector(DETECTOR_ENV, DETECTOR_SYS, DetectorBackend.RETINAFACE);
        int margin = (int) readDouble(MARGIN_ENV, MARGIN_SYS, 0);
        return new DeepFaceConfig(cos, euc, eucL2, size, align, backend, margin);
    }

    /**
     * Gets the threshold for the given distance metric.
     *
     * @param metric The distance metric.
     * @return The threshold for the given distance metric.
     */
    public double threshold(DistanceMetric metric) {
        return switch (metric) {
            case COSINE -> cosineThreshold;
            case EUCLIDEAN -> euclideanThreshold;
            case EUCLIDEAN_L2 -> euclideanL2Threshold;
        };
    }

    /**
     * Gets the threshold for the given model and distance metric.
     *
     * @param model  The model type.
     * @param metric The distance metric.
     * @return The threshold for the given model and distance metric.
     */
    public double threshold(ModelType model, DistanceMetric metric) {
        if (model == null) return threshold(metric);
        switch (model) {
            case ARCFACE:
                return switch (metric) {
                    case COSINE -> readDouble(ARCFACE_COS_ENV, ARCFACE_COS_SYS, cosineThreshold);
                    case EUCLIDEAN -> readDouble(ARCFACE_EUC_ENV, ARCFACE_EUC_SYS, euclideanThreshold);
                    case EUCLIDEAN_L2 -> readDouble(ARCFACE_EUC_L2_ENV, ARCFACE_EUC_L2_SYS, euclideanL2Threshold);
                };
            case VGG_FACE:
                return switch (metric) {
                    case COSINE -> readDouble(VGG_COS_ENV, VGG_COS_SYS, 0.68);
                    case EUCLIDEAN -> readDouble(VGG_EUC_ENV, VGG_EUC_SYS, euclideanThreshold);
                    case EUCLIDEAN_L2 -> readDouble(VGG_EUC_L2_ENV, VGG_EUC_L2_SYS, euclideanL2Threshold);
                };
            default:
                return threshold(metric);
        }
    }

    /**
     * Gets the input size for the model.
     *
     * @return The input size for the model.
     */
    public int inputSize() {
        return inputSize;
    }

    /**
     * Checks if faces should be aligned before processing.
     *
     * @return Whether to align faces before processing.
     */
    public boolean align() {
        return align;
    }

    /**
     * Checks if face detection is enforced.
     *
     * @return Whether to enforce face detection.
     */
    public boolean enforceDetection() {
        return enforceDetection;
    }

    /**
     * Gets the detector backend to use.
     *
     * @return The detector backend to use.
     */
    public DetectorBackend detectorBackend() {
        return detectorBackend;
    }

    /**
     * Gets the margin for face detection.
     *
     * @return The margin for face detection.
     */
    public int margin() {
        return margin;
    }

    /**
     * Gets the default distance metric.
     *
     * @return The default distance metric.
     */
    public DistanceMetric defaultDistanceMetric() {
        String v = System.getProperty(DISTANCE_SYS);
        if (v == null) v = System.getenv(DISTANCE_ENV);
        if (v == null || v.isBlank()) return DistanceMetric.COSINE;
        try {
            return DistanceMetric.valueOf(v.trim().toUpperCase());
        } catch (Exception e) {
            return DistanceMetric.COSINE;
        }
    }

    /**
     * Gets the path to the emotion detection model.
     *
     * @return The path to the emotion detection model.
     */
    public String emotionOnnxPath() {
        return readString(EMOTION_ENV, EMOTION_SYS, null);
    }

    /**
     * Gets the input size for the emotion detection model.
     *
     * @return The input size for the emotion detection model.
     */
    public int emotionInputSize() {
        return (int) readDouble(EMOTION_SIZE_ENV, EMOTION_SIZE_SYS, 224);
    }

    /**
     * Gets the path to the gender detection model.
     *
     * @return The path to the gender detection model.
     */
    public String genderOnnxPath() {
        return readString(GENDER_ENV, GENDER_SYS, null);
    }

    /**
     * Gets the input size for the gender detection model.
     *
     * @return The input size for the gender detection model.
     */
    public int genderInputSize() {
        return (int) readDouble(GENDER_SIZE_ENV, GENDER_SIZE_SYS, 224);
    }

    /**
     * Gets the path to the age detection model.
     *
     * @return The path to the age detection model.
     */
    public String ageOnnxPath() {
        return readString(AGE_ENV, AGE_SYS, null);
    }

    /**
     * Gets the input size for the age detection model.
     *
     * @return The input size for the age detection model.
     */
    public int ageInputSize() {
        return (int) readDouble(AGE_SIZE_ENV, AGE_SIZE_SYS, 224);
    }

    /**
     * Gets the path to the race detection model.
     *
     * @return The path to the race detection model.
     */
    public String raceOnnxPath() {
        return readString(RACE_ENV, RACE_SYS, null);
    }

    /**
     * Gets the input size for the race detection model.
     *
     * @return The input size for the race detection model.
     */
    public int raceInputSize() {
        return (int) readDouble(RACE_SIZE_ENV, RACE_SIZE_SYS, 224);
    }

    /**
     * Gets the path to the VGG-Face model.
     *
     * @return The path to the VGG-Face model.
     */
    public String vggOnnxPath() {
        return readString(VGG_ENV, VGG_SYS, null);
    }

    /**
     * Whether the framework may auto-download remote model artifacts.
     * DISABLED by default - models must be bundled in JAR via Maven build.
     *
     * @return whether auto-download is enabled.
     */
    public boolean autoDownloadEnabled() {
        return readBoolean(AUTO_DOWNLOAD_ENV, AUTO_DOWNLOAD_SYS, false);
    }

    /**
     * Connect timeout in milliseconds for model downloads. Default 10000.
     *
     * @return the connect timeout in milliseconds.
     */
    public int modelDownloadConnectTimeoutMs() {
        return (int) readDouble(DOWNLOAD_CONNECT_TIMEOUT_ENV, DOWNLOAD_CONNECT_TIMEOUT_SYS, 10000);
    }

    /**
     * Read timeout in milliseconds for model downloads. Default 20000.
     *
     * @return the read timeout in milliseconds.
     */
    public int modelDownloadReadTimeoutMs() {
        return (int) readDouble(DOWNLOAD_READ_TIMEOUT_ENV, DOWNLOAD_READ_TIMEOUT_SYS, 20000);
    }

    /**
     * Allow non-HTTPS downloads (not recommended). Defaults to false.
     *
     * @return whether insecure downloads are allowed.
     */
    public boolean allowInsecureDownloads() {
        return readBoolean(DOWNLOAD_ALLOW_INSECURE_ENV, DOWNLOAD_ALLOW_INSECURE_SYS, false);
    }

    /**
     * Gets the path to the ArcFace model.
     *
     * @return The path to the ArcFace model.
     */
    public String arcFaceOnnxPath() {
        return readString(ARCFACE_ENV, ARCFACE_SYS, null);
    }

    /**
     * Gets the input size for the ArcFace model.
     *
     * @return The input size for the ArcFace model.
     */
    public int arcFaceInputSize() {
        return (int) readDouble(ARCFACE_SIZE_ENV, ARCFACE_SIZE_SYS, 112);
    }

    /**
     * Gets the path to the FaceNet model.
     *
     * @return The path to the FaceNet model.
     */
    public String facenetOnnxPath() {
        return readString(FACENET_ENV, FACENET_SYS, null);
    }

    /**
     * Gets the input size for the FaceNet model.
     *
     * @return The input size for the FaceNet model.
     */
    public int facenetInputSize() {
        return (int) readDouble(FACENET_SIZE_ENV, FACENET_SIZE_SYS, 160);
    }

    /**
     * Gets the path to the FaceNet512 model.
     *
     * @return The path to the FaceNet512 model.
     */
    public String facenet512OnnxPath() {
        return readString(FACENET512_ENV, FACENET512_SYS, null);
    }

    /**
     * Gets the input size for the FaceNet512 model.
     *
     * @return The input size for the FaceNet512 model.
     */
    public int facenet512InputSize() {
        return (int) readDouble(FACENET512_SIZE_ENV, FACENET512_SIZE_SYS, 160);
    }

    /**
     * Gets the path to the OpenFace model.
     *
     * @return The path to the OpenFace model.
     */
    public String openfaceOnnxPath() {
        return readString(OPENFACE_ENV, OPENFACE_SYS, null);
    }

    /**
     * Gets the input size for the OpenFace model.
     *
     * @return The input size for the OpenFace model.
     */
    public int openfaceInputSize() {
        return (int) readDouble(OPENFACE_SIZE_ENV, OPENFACE_SIZE_SYS, 96);
    }

    /**
     * Gets the path to the SFace model.
     *
     * @return The path to the SFace model.
     */
    public String sfaceOnnxPath() {
        return readString(SFACE_ENV, SFACE_SYS, null);
    }

    /**
     * Gets the input size for the SFace model.
     *
     * @return The input size for the SFace model.
     */
    public int sfaceInputSize() {
        return (int) readDouble(SFACE_SIZE_ENV, SFACE_SIZE_SYS, 112);
    }

    /**
     * Gets the path to the DeepFace model.
     *
     * @return The path to the DeepFace model.
     */
    public String deepfaceOnnxPath() {
        return readString(DEEPFACE_ENV, DEEPFACE_SYS, null);
    }

    /**
     * Gets the input size for the DeepFace model.
     *
     * @return The input size for the DeepFace model.
     */
    public int deepfaceInputSize() {
        return (int) readDouble(DEEPFACE_SIZE_ENV, DEEPFACE_SIZE_SYS, 152);
    }

    /**
     * Gets the path to the RetinaFace model.
     *
     * @return The path to the RetinaFace model.
     */
    public String retinaFaceOnnxPath() {
        return readString(RETINAFACE_ENV, RETINAFACE_SYS, null);
    }

    /**
     * Gets the input size for the RetinaFace model.
     *
     * @return The input size for the RetinaFace model.
     */
    public int retinaFaceInputSize() {
        return (int) readDouble(RETINAFACE_SIZE_ENV, RETINAFACE_SIZE_SYS, 640);
    }

    /**
     * Gets the score threshold for the RetinaFace model.
     *
     * @return The score threshold for the RetinaFace model.
     */
    public double retinaFaceScoreThreshold() {
        return readDouble(RETINAFACE_SCORE_ENV, RETINAFACE_SCORE_SYS, 0.7);
    }

    /**
     * Gets the NMS threshold for the RetinaFace model.
     *
     * @return The NMS threshold for the RetinaFace model.
     */
    public double retinaFaceNmsThreshold() {
        return readDouble(RETINAFACE_NMS_ENV, RETINAFACE_NMS_SYS, 0.4);
    }

    /**
     * Returns whether ONNX model loading/inference is enabled. Defaults to true.
     *
     * @return whether ONNX is enabled.
     */
    public boolean onnxEnabled() {
        return readBoolean(ONNX_ENABLED_ENV, ONNX_ENABLED_SYS, true);
    }

    /**
     * Normalization mean per channel as three comma-separated values (R,G,B) in 0..1 range. Default 0,0,0.
     *
     * @return the normalization mean.
     */
    public double[] normalizationMean() {
        return readDoubles(NORM_MEAN_ENV, NORM_MEAN_SYS, new double[]{0.0, 0.0, 0.0});
    }

    /**
     * Normalization std per channel as three comma-separated values (R,G,B). Default 1,1,1.
     *
     * @return the normalization standard deviation.
     */
    public double[] normalizationStd() {
        return readDoubles(NORM_STD_ENV, NORM_STD_SYS, new double[]{1.0, 1.0, 1.0});
    }

    /**
     * Color space for model input: "RGB" or "BGR". Defaults to RGB.
     *
     * @return the color space.
     */
    public String colorSpace() {
        return readString(COLORSPACE_ENV, COLORSPACE_SYS, "RGB");
    }

    /**
     * Returns model-specific landmark template points for a 112px reference crop as
     * an array {leftEyeX,leftEyeY,rightEyeX,rightEyeY,noseX,noseY}.
     * Values can be overridden via environment/system properties as comma-separated list.
     *
     * @param model The model type.
     * @return the template points.
     */
    public double[] templatePoints(io.github.codesapienbe.springvision.facebytes.enums.ModelType model) {
        if (model == null) model = io.github.codesapienbe.springvision.facebytes.enums.ModelType.ARCFACE;
        switch (model) {
            case SFACE:
                return readDoublesFull(SFACE_TEMPLATE_ENV, SFACE_TEMPLATE_SYS, new double[]{36.0, 50.0, 76.0, 50.0, 56.0, 74.0});
            case VGG_FACE:
                return readDoublesFull(VGG_TEMPLATE_ENV, VGG_TEMPLATE_SYS, new double[]{34.0, 48.0, 78.0, 48.0, 56.0, 70.0});
            case ARCFACE:
            default:
                return readDoublesFull(ARCFACE_TEMPLATE_ENV, ARCFACE_TEMPLATE_SYS, new double[]{38.2946, 51.6963, 73.5318, 51.5014, 56.0252, 71.7366});
        }
    }

    private static String readString(String env, String sys, String def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static double readDouble(String env, String sys, double def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean readBoolean(String env, String sys, boolean def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        v = v.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }

    private static DetectorBackend readDetector(String env, String sys, DetectorBackend def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        try {
            return DetectorBackend.valueOf(v.trim().toUpperCase());
        } catch (Exception e) {
            return def;
        }
    }

    private static double[] readDoubles(String env, String sys, double[] def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        try {
            String[] parts = v.split(",");
            double[] out = new double[Math.min(parts.length, 3)];
            for (int i = 0; i < out.length; i++) out[i] = Double.parseDouble(parts[i].trim());
            if (out.length < 3) {
                double[] full = new double[3];
                System.arraycopy(out, 0, full, 0, out.length);
                for (int i = out.length; i < 3; i++) full[i] = out[out.length - 1];
                return full;
            }
            return out;
        } catch (Exception e) {
            return def;
        }
    }

    private static double[] readDoublesFull(String env, String sys, double[] def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        try {
            String[] parts = v.split(",");
            double[] out = new double[Math.max(def.length, parts.length)];
            for (int i = 0; i < out.length; i++)
                out[i] = (i < parts.length && !parts[i].isBlank()) ? Double.parseDouble(parts[i].trim()) : (i < def.length ? def[i] : 0.0);
            return out;
        } catch (Exception e) {
            return def;
        }
    }
}
