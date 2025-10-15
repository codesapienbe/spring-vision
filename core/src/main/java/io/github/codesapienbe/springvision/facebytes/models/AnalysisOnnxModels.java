package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;

/**
 * Lazy singletons for analysis ONNX models.
 */
public final class AnalysisOnnxModels {

    private static volatile OnnxSimpleModel emotion;
    private static volatile OnnxSimpleModel gender;
    private static volatile OnnxSimpleModel age;
    private static volatile OnnxSimpleModel race;

    private AnalysisOnnxModels() {
    }

    /**
     * Gets or creates the emotion analysis ONNX model.
     *
     * @return the emotion ONNX model
     * @throws Exception if the model cannot be loaded
     */
    public static OnnxSimpleModel getEmotion() throws Exception {
        if (emotion == null) {
            synchronized (AnalysisOnnxModels.class) {
                if (emotion == null) {
                    String path = DeepFaceConfig.current().emotionOnnxPath();
                    if (path == null) throw new IllegalStateException("Emotion ONNX path not configured");
                    emotion = new OnnxSimpleModel(path);
                }
            }
        }
        return emotion;
    }

    /**
     * Gets or creates the gender analysis ONNX model.
     *
     * @return the gender ONNX model
     * @throws Exception if the model cannot be loaded
     */
    public static OnnxSimpleModel getGender() throws Exception {
        if (gender == null) {
            synchronized (AnalysisOnnxModels.class) {
                if (gender == null) {
                    String path = DeepFaceConfig.current().genderOnnxPath();
                    if (path == null) throw new IllegalStateException("Gender ONNX path not configured");
                    gender = new OnnxSimpleModel(path);
                }
            }
        }
        return gender;
    }

    /**
     * Gets or creates the age analysis ONNX model.
     *
     * @return the age ONNX model
     * @throws Exception if the model cannot be loaded
     */
    public static OnnxSimpleModel getAge() throws Exception {
        if (age == null) {
            synchronized (AnalysisOnnxModels.class) {
                if (age == null) {
                    String path = DeepFaceConfig.current().ageOnnxPath();
                    if (path == null) throw new IllegalStateException("Age ONNX path not configured");
                    age = new OnnxSimpleModel(path);
                }
            }
        }
        return age;
    }

    /**
     * Gets or creates the race analysis ONNX model.
     *
     * @return the race ONNX model
     * @throws Exception if the model cannot be loaded
     */
    public static OnnxSimpleModel getRace() throws Exception {
        if (race == null) {
            synchronized (AnalysisOnnxModels.class) {
                if (race == null) {
                    String path = DeepFaceConfig.current().raceOnnxPath();
                    if (path == null) throw new IllegalStateException("Race ONNX path not configured");
                    race = new OnnxSimpleModel(path);
                }
            }
        }
        return race;
    }

    /**
     * Attempts to initialize configured models to catch errors early and warm caches.
     * Missing model paths are ignored.
     */
    public static void warmupIfConfigured() {
        try {
            if (DeepFaceConfig.current().emotionOnnxPath() != null) getEmotion();
        } catch (Throwable ignored) {
        }
        try {
            if (DeepFaceConfig.current().genderOnnxPath() != null) getGender();
        } catch (Throwable ignored) {
        }
        try {
            if (DeepFaceConfig.current().ageOnnxPath() != null) getAge();
        } catch (Throwable ignored) {
        }
        try {
            if (DeepFaceConfig.current().raceOnnxPath() != null) getRace();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Closes all initialized models and releases native resources.
     */
    public static void shutdown() {
        try {
            if (emotion != null) {
                emotion.close();
                emotion = null;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (gender != null) {
                gender.close();
                gender = null;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (age != null) {
                age.close();
                age = null;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (race != null) {
                race.close();
                race = null;
            }
        } catch (Throwable ignored) {
        }
    }
}
