package io.github.codesapienbe.springvision.facebytes.utils;

import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;
// Use fully-qualified logging references to avoid unused-import linter warnings

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static final String CACHE_DIR = System.getProperty("user.home") + "/.facebytes/models";

    // Use ModelUrls for canonical URLs and checksum map
    private static final java.util.Map<String, String> MODEL_URLS = io.github.codesapienbe.springvision.facebytes.models.ModelUrls.defaults();
    // Disable checksum validation for now per user request. Keep the hook for future re-enable.
    private static final java.util.Map<String, String> MODEL_CHECKSUMS = java.util.Collections.emptyMap();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private ModelDownloader() {
    }

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
        String fileName = getModelFileName(modelType);
        Path targetPath = Paths.get(CACHE_DIR, fileName);
        String url = getModelUrl(modelType);
        String expectedChecksum = MODEL_CHECKSUMS.getOrDefault(fileName, null);

        try {
            // Create cache directory if it doesn't exist
            Files.createDirectories(Paths.get(CACHE_DIR));

            Logs.info("ModelDownloader", "model.download_start", Map.of("model", modelType.name(), "url", url));

            // Download with HTTP client (timeout, redirects) and strict HTTPS guard
            downloadWithProgress(url, targetPath, expectedChecksum);

            // Verify model integrity
            if (!verifyModelIntegrity(targetPath, modelType)) {
                Files.deleteIfExists(targetPath);
                try {
                    Files.deleteIfExists(targetPath.resolveSibling(targetPath.getFileName().toString() + ".sha256"));
                } catch (Exception ignored) {
                }
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
     * @param url        the URL to download from
     * @param targetPath the local path to save to
     * @throws IOException if download fails
     */
    private static void downloadWithProgress(String url, Path targetPath, String expectedChecksum) throws IOException {
        if (url == null || url.isBlank()) throw new IOException("No URL configured for model");
        if (!url.startsWith("https://")) throw new IOException("Model URL must use HTTPS: " + url);

        // Candidate sources: primary URL first, then simple mirror patterns with the same filename.
        java.util.List<String> candidateUrls = new java.util.ArrayList<>();
        candidateUrls.add(url);

        // Common alternate hosts / mirrors to try if the primary source fails. These are best-effort
        // fallbacks — we append the filename to each mirror base to form a candidate URL.
        String[] mirrorBases = new String[]{
            "https://huggingface.co/serengil/deepface_models/resolve/main/",
            "https://storage.googleapis.com/deepface_models/",
            "https://onnxzoo.blob.core.windows.net/models/",
            "https://download.openvinotoolkit.org/models/"
        };

        String fileName = targetPath.getFileName().toString();
        for (String base : mirrorBases) {
            String candidate = base.endsWith("/") ? base + fileName : base + "/" + fileName;
            if (!candidateUrls.contains(candidate)) candidateUrls.add(candidate);
        }

        IOException lastException = null;
        final int MAX_ATTEMPTS_PER_URL = 3;

        for (String candidate : candidateUrls) {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_URL; attempt++) {
                try {
                    Logs.info("ModelDownloader", "model.download_attempt", Map.of("url", candidate, "attempt", attempt));

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(candidate))
                        .timeout(Duration.ofSeconds(120))
                        .GET()
                        .build();

                    HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() != 200) {
                        throw new IOException("Failed to download model: HTTP " + response.statusCode());
                    }

                    byte[] body = response.body();
                    if (body == null || body.length == 0) throw new IOException("Downloaded model is empty");

                    // Write to a temporary file first to ensure atomicity. Use .tmp suffix.
                    Path tmpPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
                    try {
                        Files.write(tmpPath, body, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

                        // Compute checksum of the temp file
                        String actual = calculateSHA256(tmpPath);
                        Path sidecar = tmpPath.resolveSibling(tmpPath.getFileName().toString() + ".sha256");
                        try {
                            Files.writeString(sidecar, actual);
                        } catch (Exception ignored) {
                        }

                        // If expected checksum provided and mismatches, remove temp and continue
                        if (expectedChecksum != null && !expectedChecksum.isBlank() && !expectedChecksum.equalsIgnoreCase(actual)) {
                            try {
                                Files.deleteIfExists(tmpPath);
                            } catch (Exception ignored) {
                            }
                            lastException = new IOException("Downloaded model checksum mismatch (expected vs actual)");
                            Logs.warn("ModelDownloader", "model.checksum_mismatch_during_download", Map.of("expected", expectedChecksum, "actual", actual));
                            throw lastException;
                        }

                        // Atomically move temp into place
                        try {
                            Files.move(tmpPath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                        } catch (Exception moveEx) {
                            // Fallback to non-atomic move if atomic not supported on platform
                            Files.move(tmpPath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Write persistent sidecar checksum next to final file
                        Path finalSidecar = targetPath.resolveSibling(targetPath.getFileName().toString() + ".sha256");
                        try {
                            Files.writeString(finalSidecar, actual);
                        } catch (Exception ignored) {
                        }

                        // success
                        return;
                    } finally {
                        // Ensure tmp cleanup in case of failures
                        try {
                            Files.deleteIfExists(targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp"));
                        } catch (Exception ignored) {
                        }
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted", ie);
                } catch (IOException ioe) {
                    lastException = ioe;
                    Logs.warn("ModelDownloader", "model.download_retry", Map.of("url", candidate, "attempt", attempt, "error", ioe.getMessage()));
                    // exponential backoff
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie2) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted", ie2);
                    }
                } catch (Exception e) {
                    lastException = new IOException("Failed to download model: " + e.getMessage(), e);
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie2) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted", ie2);
                    }
                }
            }
        }

        if (lastException != null) throw lastException;
        throw new IOException("Failed to download model from any source");
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
        String fileName = getModelFileName(modelType);
        return MODEL_CHECKSUMS.getOrDefault(fileName, null);
    }

    /**
     * Gets the download URL for a model type.
     *
     * @param modelType the type of model
     * @return the download URL
     * @throws DeepFaceException if model type is not supported
     */
    private static String getModelUrl(ModelType modelType) throws DeepFaceException {
        String fileName = getModelFileName(modelType);
        String url = MODEL_URLS.get(fileName);
        if (url == null) throw new DeepFaceException("No URL configured for model: " + fileName);
        return url;
    }

    /**
     * Gets the filename for a model type.
     *
     * @param modelType the type of model
     * @return the filename
     */
    private static String getModelFileName(ModelType modelType) {
        return switch (modelType) {
            case VGG_FACE -> "vggface.onnx";
            case ARCFACE -> "arcface.onnx";
            case FACENET, FACENET512 -> "facenet128.onnx";
            case OPEN_FACE -> "openface.onnx";
            case DEEPID -> "deepid.onnx";
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
