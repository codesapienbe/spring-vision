package com.deepface.config;

import com.deepface.enums.DetectorBackend;
import com.deepface.enums.DistanceMetric;

public final class DeepFaceConfig {

    private static final String COSINE_ENV = "FACEBYTES_THRESHOLD_COSINE";
    private static final String EUCLIDEAN_ENV = "FACEBYTES_THRESHOLD_EUCLIDEAN";
    private static final String EUCLIDEAN_L2_ENV = "FACEBYTES_THRESHOLD_EUCLIDEAN_L2";

    private static final String COSINE_SYS = "facebytes.threshold.cosine";
    private static final String EUCLIDEAN_SYS = "facebytes.threshold.euclidean";
    private static final String EUCLIDEAN_L2_SYS = "facebytes.threshold.euclidean_l2";

    private static final String INPUT_SIZE_ENV = "FACEBYTES_INPUT_SIZE";
    private static final String INPUT_SIZE_SYS = "facebytes.input.size";

    private static final String ALIGN_ENV = "FACEBYTES_ALIGN";
    private static final String ALIGN_SYS = "facebytes.align";

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

    private final double cosineThreshold;
    private final double euclideanThreshold;
    private final double euclideanL2Threshold;
    private final int inputSize;
    private final boolean align;
    private final DetectorBackend detectorBackend;
    private final int margin;

    private DeepFaceConfig(double cosineThreshold, double euclideanThreshold, double euclideanL2Threshold,
                           int inputSize, boolean align, DetectorBackend detectorBackend, int margin) {
        this.cosineThreshold = cosineThreshold;
        this.euclideanThreshold = euclideanThreshold;
        this.euclideanL2Threshold = euclideanL2Threshold;
        this.inputSize = inputSize;
        this.align = align;
        this.detectorBackend = detectorBackend;
        this.margin = Math.max(0, margin);
    }

    public static DeepFaceConfig current() {
        double cos = readDouble(COSINE_ENV, COSINE_SYS, 0.68);
        double euc = readDouble(EUCLIDEAN_ENV, EUCLIDEAN_SYS, 0.60);
        double eucL2 = readDouble(EUCLIDEAN_L2_ENV, EUCLIDEAN_L2_SYS, 1.13);
        int size = (int) readDouble(INPUT_SIZE_ENV, INPUT_SIZE_SYS, 224);
        boolean align = readBoolean(ALIGN_ENV, ALIGN_SYS, true);
        DetectorBackend backend = readDetector(DETECTOR_ENV, DETECTOR_SYS, DetectorBackend.OPENCV);
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

    public int inputSize() { return inputSize; }
    public boolean align() { return align; }
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
