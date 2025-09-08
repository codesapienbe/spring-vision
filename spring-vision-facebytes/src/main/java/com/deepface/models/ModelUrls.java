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

    /**
     * Optional SHA-256 checksums for remote model artifacts. When present, downloads will be
     * validated against these values. Populate with authoritative checksums as models are released.
     */
    public static Map<String, String> checksums() {
        // NOTE: These values are example placeholders. Replace with authoritative SHA-256
        // hex strings for the listed model files before enabling strict checksum validation.
        return Map.ofEntries(
            Map.entry("arcface.onnx", "6f1d2f7b3a8c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e"),
            Map.entry("facenet128.onnx", "d1c2b3a4f5e6d7c8b9a0f1e2d3c4b5a6f7e8d9c0b1a2f3e4d5c6b7a8f9e0d1c2"),
            Map.entry("openface.onnx", "a1b2c3d4e5f60718293a4b5c6d7e8f9a0b1c2d3e4f5061728394a5b6c7d8e9f0"),
            Map.entry("deepid.onnx", "0f9e8d7c6b5a4938271605f4e3d2c1b0a9f8e7d6c5b4a3928173654f3e2d1c0b"),
            Map.entry("vggface.onnx", "3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a29181726354433221100ff")
        );
    }
}
