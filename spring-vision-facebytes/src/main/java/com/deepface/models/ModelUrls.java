package com.deepface.models;

import java.util.Map;

public final class ModelUrls {

    private ModelUrls() {}

    public static Map<String, String> defaults() {
        return Map.of(
            "arcface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/ArcFace.onnx",
            "facenet128.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/Facenet128.onnx",
            "openface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/OpenFace.onnx",
            "deepid.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/DeepID.onnx",
            "vggface.onnx", "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/VGGFace.onnx"
        );
    }
}
