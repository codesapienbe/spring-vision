package io.github.codesapienbe.springvision.facebytes.models;

import java.util.Map;

/**
 * Model URLs and classpath locations for FaceBytes ONNX models.
 * Models are downloaded during Maven build and bundled in JAR.
 * No runtime downloads - all models must be present in classpath.
 */
public final class ModelUrls {

    private ModelUrls() {
    }

    /**
     * Classpath locations for ONNX models bundled in JAR.
     * These paths correspond to models downloaded by maven-download-plugin during build.
     */
    public static Map<String, String> classpathLocations() {
        return Map.ofEntries(
            // Face recognition embedding models
            Map.entry("arcface.onnx", "/models/facebytes/arcface.onnx"),
            Map.entry("facenet128.onnx", "/models/facebytes/facenet128.onnx"),
            Map.entry("sface.onnx", "/models/facebytes/sface.onnx"),

            // Face detector models
            Map.entry("yunet.onnx", "/models/facebytes/yunet.onnx"),
            Map.entry("retinaface.onnx", "/models/facebytes/yunet.onnx"), // alias

            // Facial analysis models
            Map.entry("age_model.onnx", "/models/facebytes/age_model.onnx"),
            Map.entry("gender_model.onnx", "/models/facebytes/gender_model.onnx"),
            Map.entry("emotion_model.onnx", "/models/facebytes/emotion_model.onnx")
        );
    }

    /**
     * @deprecated Models are now bundled in JAR from classpath. This method is kept for reference only.
     * Use classpathLocations() instead.
     */
    @Deprecated(forRemoval = true)
    public static Map<String, String> defaults() {
        return Map.ofEntries(
            // ONNX models for face recognition embeddings
            Map.entry("arcface.onnx", "https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"),
            Map.entry("facenet128.onnx", "https://storage.googleapis.com/ailia-models/facenet/facenet.onnx"),
            Map.entry("sface.onnx", "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"),

            // Face detector models
            Map.entry("retinaface.onnx", "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"),

            // Facial analysis ONNX models
            Map.entry("age_model.onnx", "https://github.com/onnx/models/raw/main/vision/body_analysis/age_gender/models/age_googlenet.onnx"),
            Map.entry("gender_model.onnx", "https://github.com/onnx/models/raw/main/vision/body_analysis/age_gender/models/gender_googlenet.onnx"),
            Map.entry("emotion_model.onnx", "https://github.com/onnx/models/raw/main/vision/body_analysis/emotion_ferplus/model/emotion-ferplus-8.onnx")
        );
    }

    /**
     * @deprecated Checksum validation not needed for classpath-bundled models.
     */
    @Deprecated(forRemoval = true)
    public static Map<String, String> checksums() {
        return Map.of();
    }
}
