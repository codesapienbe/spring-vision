package com.deepface.models;

import java.util.Map;

public final class ModelUrls {

    private ModelUrls() {}

    public static Map<String, String> defaults() {
        return Map.ofEntries(
            Map.entry("arcface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/ArcFace.onnx"),
            Map.entry("facenet128.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/Facenet128.onnx"),
            Map.entry("openface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/OpenFace.onnx"),
            Map.entry("deepid.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/DeepID.onnx"),
            Map.entry("vggface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/VGGFace.onnx"),

            // DeepFace Keras model artifacts (source: serengil/deepface_models)
            Map.entry("age_model_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/age_model_weights.h5"),
            Map.entry("arcface_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/arcface_weights.h5"),
            Map.entry("deepid_keras_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/deepid_keras_weights.h5"),
            Map.entry("facenet512_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/facenet512_weights.h5"),
            Map.entry("facenet_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/facenet_weights.h5"),
            Map.entry("facial_expression_model_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/facial_expression_model_weights.h5"),
            Map.entry("gender_model_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/gender_model_weights.h5"),
            Map.entry("openface_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/openface_weights.h5"),
            Map.entry("race_model_single_batch.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/race_model_single_batch.h5"),
            Map.entry("retinaface.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/retinaface.h5"),
            Map.entry("vgg_face_weights.h5", "https://github.com/serengil/deepface_models/releases/download/v1.0/vgg_face_weights.h5")
        );
    }

    /**
     * Optional SHA-256 checksums for remote model artifacts. When present, downloads will be
     * validated against these values. Populate with authoritative checksums as models are released.
     */
    public static Map<String, String> checksums() {
        // NOTE: These values are example placeholders (64-char hex). Replace with
        // authoritative SHA-256 hex strings for the listed model files before
        // enabling strict checksum validation.
        final String placeholder64 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        return Map.ofEntries(
            Map.entry("arcface.onnx", placeholder64),
            Map.entry("facenet128.onnx", placeholder64),
            Map.entry("openface.onnx", placeholder64),
            Map.entry("deepid.onnx", placeholder64),
            Map.entry("vggface.onnx", placeholder64),

            Map.entry("age_model_weights.h5", placeholder64),
            Map.entry("arcface_weights.h5", placeholder64),
            Map.entry("deepid_keras_weights.h5", placeholder64),
            Map.entry("facenet512_weights.h5", placeholder64),
            Map.entry("facenet_weights.h5", placeholder64),
            Map.entry("facial_expression_model_weights.h5", placeholder64),
            Map.entry("gender_model_weights.h5", placeholder64),
            Map.entry("openface_weights.h5", placeholder64),
            Map.entry("race_model_single_batch.h5", placeholder64),
            Map.entry("retinaface.h5", placeholder64),
            Map.entry("vgg_face_weights.h5", placeholder64)
        );
    }
}
