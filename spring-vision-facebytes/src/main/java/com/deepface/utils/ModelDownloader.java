package com.deepface.utils;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.enums.ModelType;
import com.deepface.utils.Logs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for downloading and managing ONNX models for FaceBytes.
 * Provides automatic model download, caching, and integrity verification.
 * 
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class ModelDownloader {

    private static final String MODELS_DIR = "models";
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.facebytes/models";
    
    // Pre-trained model URLs (these are example URLs - replace with actual model sources)
    private static final String VGG_FACE_URL = "https://github.com/face-recognition-models/face-recognition-models/raw/main/vggface2.onnx";
    private static final String ARCFACE_URL = "https://github.com/face-recognition-models/face-recognition-models/raw/main/arcface.onnx";
    private static final String FACENET_URL = "https://github.com/face-recognition-models/face-recognition-models/raw/main/facenet.onnx";
    
    // Model checksums for integrity verification
    private static final String VGG_FACE_SHA256 = "a1b2c3d4e5f6..."; // Placeholder
    private static final String ARCFACE_SHA256 = "b2c3d4e5f6a1..."; // Placeholder
    private static final String FACENET_SHA256 = "c3d4e5f6a1b2..."; // Placeholder
    
    private static final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private ModelDownloader() {}

    /**
     * Ensures the specified model is available, downloading it if necessary.
     * 
     * @param modelType the type of model to ensure availability
     * @return the path to the model file
     * @throws DeepFaceException if the model cannot be downloaded or verified
     */
    public static String ensureModelAvailable(ModelType modelType) throws DeepFaceException {
        String modelPath = getModelPath(modelType);
        if (Files.exists(Paths.get(modelPath))) {
            Logs.info("ModelDownloader", "model.already_exists", Map.of("model", modelType.name(), "path", modelPath));
            return modelPath;
        }
        
        if (downloadInProgress.compareAndSet(false, true)) {
            try {
                downloadModel(modelType);
                return getModelPath(modelType);
            } finally {
                downloadInProgress.set(false);
            }
        } else {
            // Wait for download to complete
            while (downloadInProgress.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DeepFaceException("Model download interrupted", e);
                }
            }
            return getModelPath(modelType);
        }
    }

    /**
     * Downloads a model asynchronously without blocking.
     * 
     * @param modelType the type of model to download
     * @return a CompletableFuture that completes when download is finished
     */
    public static CompletableFuture<String> downloadModelAsync(ModelType modelType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureModelAvailable(modelType);
                return getModelPath(modelType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to download model: " + modelType, e);
            }
        });
    }

    /**
     * Downloads the specified model to the local cache.
     * 
     * @param modelType the type of model to download
     * @throws DeepFaceException if download fails
     */
    private static void downloadModel(ModelType modelType) throws DeepFaceException {
        String url = getModelUrl(modelType);
        String fileName = getModelFileName(modelType);
        Path targetPath = Paths.get(CACHE_DIR, fileName);
        
        try {
            // Create cache directory if it doesn't exist
            Files.createDirectories(Paths.get(CACHE_DIR));
            
            Logs.info("ModelDownloader", "model.download_start", Map.of("model", modelType.name(), "url", url));
            
            // Download with progress tracking
            downloadWithProgress(url, targetPath);
            
            // Verify model integrity
            if (!verifyModelIntegrity(targetPath, modelType)) {
                Files.deleteIfExists(targetPath);
                throw new DeepFaceException("Model integrity check failed for " + modelType);
            }
            
            Logs.info("ModelDownloader", "model.download_complete", Map.of("model", modelType.name(), "path", targetPath.toString()));
            
        } catch (IOException e) {
            Logs.error("ModelDownloader", "model.download_failed", e, Map.of("model", modelType.name(), "url", url));
            throw new DeepFaceException("Failed to download model: " + modelType, e);
        }
    }

    /**
     * Downloads a file with progress tracking and error handling.
     * 
     * @param url the URL to download from
     * @param targetPath the local path to save to
     * @throws IOException if download fails
     */
    private static void downloadWithProgress(String url, Path targetPath) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Verifies the integrity of a downloaded model using SHA-256 checksum.
     * 
     * @param modelPath the path to the model file
     * @param modelType the type of model being verified
     * @return true if integrity check passes, false otherwise
     */
    private static boolean verifyModelIntegrity(Path modelPath, ModelType modelType) {
        try {
            String expectedChecksum = getExpectedChecksum(modelType);
            if (expectedChecksum == null || expectedChecksum.length() < 10) {
                // Skip verification if no valid checksum is available
                Logs.warn("ModelDownloader", "model.checksum_skip", Map.of("model", modelType.name()));
                return true;
            }
            
            String actualChecksum = calculateSHA256(modelPath);
            boolean isValid = expectedChecksum.equalsIgnoreCase(actualChecksum);
            
            if (!isValid) {
                Logs.error("ModelDownloader", "model.checksum_mismatch", null, Map.of(
                    "model", modelType.name(),
                    "expected", expectedChecksum,
                    "actual", actualChecksum
                ));
            }
            
            return isValid;
        } catch (Exception e) {
            Logs.error("ModelDownloader", "model.checksum_error", e, Map.of("model", modelType.name()));
            return false;
        }
    }

    /**
     * Calculates SHA-256 checksum of a file.
     * 
     * @param filePath the path to the file
     * @return the hexadecimal SHA-256 checksum
     * @throws IOException if file reading fails
     */
    private static String calculateSHA256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(filePath));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Failed to calculate checksum", e);
        }
    }

    /**
     * Gets the expected checksum for a model type.
     * 
     * @param modelType the type of model
     * @return the expected SHA-256 checksum, or null if not available
     */
    private static String getExpectedChecksum(ModelType modelType) {
        return switch (modelType) {
            case VGG_FACE -> VGG_FACE_SHA256;
            case ARCFACE -> ARCFACE_SHA256;
            case FACENET -> FACENET_SHA256;
            default -> null;
        };
    }

    /**
     * Gets the download URL for a model type.
     * 
     * @param modelType the type of model
     * @return the download URL
     * @throws DeepFaceException if model type is not supported
     */
    private static String getModelUrl(ModelType modelType) throws DeepFaceException {
        return switch (modelType) {
            case VGG_FACE -> VGG_FACE_URL;
            case ARCFACE -> ARCFACE_URL;
            case FACENET -> FACENET_URL;
            default -> throw new DeepFaceException("Unsupported model type for download: " + modelType);
        };
    }

    /**
     * Gets the filename for a model type.
     * 
     * @param modelType the type of model
     * @return the filename
     */
    private static String getModelFileName(ModelType modelType) {
        return switch (modelType) {
            case VGG_FACE -> "vggface2.onnx";
            case ARCFACE -> "arcface.onnx";
            case FACENET -> "facenet.onnx";
            default -> modelType.name().toLowerCase() + ".onnx";
        };
    }

    /**
     * Gets the local path where a model should be stored.
     * 
     * @param modelType the type of model
     * @return the local file path
     */
    private static String getModelPath(ModelType modelType) {
        String fileName = getModelFileName(modelType);
        return Paths.get(CACHE_DIR, fileName).toString();
    }

    /**
     * Cleans up old or corrupted model files.
     * 
     * @param modelType the type of model to clean up
     */
    public static void cleanupModel(ModelType modelType) {
        try {
            Path modelPath = Paths.get(getModelPath(modelType));
            if (Files.exists(modelPath)) {
                Files.delete(modelPath);
                Logs.info("ModelDownloader", "model.cleanup", Map.of("model", modelType.name()));
            }
        } catch (IOException e) {
            Logs.warn("ModelDownloader", "model.cleanup_failed", Map.of("model", modelType.name(), "error", e.getMessage()));
        }
    }

    /**
     * Gets the cache directory path.
     * 
     * @return the cache directory path
     */
    public static String getCacheDirectory() {
        return CACHE_DIR;
    }
} 