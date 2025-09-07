package com.deepface.config;

import com.deepface.enums.DetectorBackend;
import com.deepface.enums.DistanceMetric;
import com.deepface.enums.ModelType;

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

    public double threshold(DistanceMetric metric) {
        return switch (metric) {
            case COSINE -> cosineThreshold;
            case EUCLIDEAN -> euclideanThreshold;
            case EUCLIDEAN_L2 -> euclideanL2Threshold;
        };
    }

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

    public int inputSize() { return inputSize; }
    public boolean align() { return align; }
    public boolean enforceDetection() { return enforceDetection; }
    public DetectorBackend detectorBackend() { return detectorBackend; }
    public int margin() { return margin; }

    public DistanceMetric defaultDistanceMetric() {
        String v = System.getProperty(DISTANCE_SYS);
        if (v == null) v = System.getenv(DISTANCE_ENV);
        if (v == null || v.isBlank()) return DistanceMetric.COSINE;
        try { return DistanceMetric.valueOf(v.trim().toUpperCase()); } catch (Exception e) { return DistanceMetric.COSINE; }
    }

    public String emotionOnnxPath() { return readString(EMOTION_ENV, EMOTION_SYS, null); }
    public int emotionInputSize() { return (int) readDouble(EMOTION_SIZE_ENV, EMOTION_SIZE_SYS, 224); }

    public String genderOnnxPath() { return readString(GENDER_ENV, GENDER_SYS, null); }
    public int genderInputSize() { return (int) readDouble(GENDER_SIZE_ENV, GENDER_SIZE_SYS, 224); }

    public String ageOnnxPath() { return readString(AGE_ENV, AGE_SYS, null); }
    public int ageInputSize() { return (int) readDouble(AGE_SIZE_ENV, AGE_SIZE_SYS, 224); }

    public String raceOnnxPath() { return readString(RACE_ENV, RACE_SYS, null); }
    public int raceInputSize() { return (int) readDouble(RACE_SIZE_ENV, RACE_SIZE_SYS, 224); }

    public String vggOnnxPath() { return readString(VGG_ENV, VGG_SYS, null); }

    public String arcFaceOnnxPath() { return readString(ARCFACE_ENV, ARCFACE_SYS, null); }
    public int arcFaceInputSize() { return (int) readDouble(ARCFACE_SIZE_ENV, ARCFACE_SIZE_SYS, 112); }

    public String facenetOnnxPath() { return readString(FACENET_ENV, FACENET_SYS, null); }
    public int facenetInputSize() { return (int) readDouble(FACENET_SIZE_ENV, FACENET_SIZE_SYS, 160); }

    public String facenet512OnnxPath() { return readString(FACENET512_ENV, FACENET512_SYS, null); }
    public int facenet512InputSize() { return (int) readDouble(FACENET512_SIZE_ENV, FACENET512_SIZE_SYS, 160); }

    public String openfaceOnnxPath() { return readString(OPENFACE_ENV, OPENFACE_SYS, null); }
    public int openfaceInputSize() { return (int) readDouble(OPENFACE_SIZE_ENV, OPENFACE_SIZE_SYS, 96); }

    public String sfaceOnnxPath() { return readString(SFACE_ENV, SFACE_SYS, null); }
    public int sfaceInputSize() { return (int) readDouble(SFACE_SIZE_ENV, SFACE_SIZE_SYS, 112); }

    public String deepfaceOnnxPath() { return readString(DEEPFACE_ENV, DEEPFACE_SYS, null); }
    public int deepfaceInputSize() { return (int) readDouble(DEEPFACE_SIZE_ENV, DEEPFACE_SIZE_SYS, 152); }

    public String retinaFaceOnnxPath() { return readString(RETINAFACE_ENV, RETINAFACE_SYS, null); }
    public int retinaFaceInputSize() { return (int) readDouble(RETINAFACE_SIZE_ENV, RETINAFACE_SIZE_SYS, 640); }
    public double retinaFaceScoreThreshold() { return readDouble(RETINAFACE_SCORE_ENV, RETINAFACE_SCORE_SYS, 0.7); }
    public double retinaFaceNmsThreshold() { return readDouble(RETINAFACE_NMS_ENV, RETINAFACE_NMS_SYS, 0.4); }

    /**
     * Returns whether ONNX model loading/inference is enabled. Defaults to true.
     */
    public boolean onnxEnabled() { return readBoolean(ONNX_ENABLED_ENV, ONNX_ENABLED_SYS, true); }

    private static String readString(String env, String sys, String def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static double readDouble(String env, String sys, double def) {
        String v = System.getProperty(sys);
        if (v == null) v = System.getenv(env);
        if (v == null || v.isBlank()) return def;
        try { return Double.parseDouble(v.trim()); } catch (Exception e) { return def; }
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
        try { return DetectorBackend.valueOf(v.trim().toUpperCase()); } catch (Exception e) { return def; }
    }
}
