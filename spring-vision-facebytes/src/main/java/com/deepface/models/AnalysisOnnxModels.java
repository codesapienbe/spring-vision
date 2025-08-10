package com.deepface.models;

import com.deepface.config.DeepFaceConfig;

/**
 * Lazy singletons for analysis ONNX models.
 */
public final class AnalysisOnnxModels {

    private static volatile OnnxSimpleModel emotion;
    private static volatile OnnxSimpleModel gender;
    private static volatile OnnxSimpleModel age;
    private static volatile OnnxSimpleModel race;

    private AnalysisOnnxModels() {}

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
}
