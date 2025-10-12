package io.github.codesapienbe.springvision.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Universal model resource loader that prioritizes classpath resources over external downloads.
 * This ensures portability for Docker deployments and Maven releases.
 *
 * <p>Loading priority:</p>
 * <ol>
 *   <li>Classpath resources (bundled in JAR)</li>
 *   <li>Explicitly configured external path</li>
 *   <li>User cache directory (~/.spring-vision/models)</li>
 *   <li>Auto-download from URL (if enabled)</li>
 * </ol>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public final class ModelResourceLoader {

    private static final Logger logger = LoggerFactory.getLogger(ModelResourceLoader.class);

    private ModelResourceLoader() {
    }

    /**
     * Resolves a model file path, trying classpath first, then external locations.
     *
     * @param configuredPath    the explicitly configured path (may be null)
     * @param classpathResource the classpath resource path (e.g., "/models/model.onnx")
     * @param modelName         the model filename for the cache directory
     * @param moduleSubdir      the module subdirectory in cache (e.g., "opencv", "yolo")
     * @param downloadUrl       the URL to download from if model not found (may be null)
     * @param autoDownload      whether to auto-download if not found
     * @return absolute path to the model file, or null if not available
     */
    public static String resolveModelPath(
        String configuredPath,
        String classpathResource,
        String modelName,
        String moduleSubdir,
        String downloadUrl,
        boolean autoDownload) {

        // 1) Try classpath resources first (highest priority for portability)
        String classpathPath = tryLoadFromClasspath(classpathResource, modelName, moduleSubdir);
        if (classpathPath != null) {
            logger.debug("Loaded model from classpath: resource={}, path={}", classpathResource, classpathPath);
            return classpathPath;
        }

        // 2) Check explicitly configured path
        if (configuredPath != null && !configuredPath.isBlank() && !configuredPath.startsWith("classpath:")) {
            String expandedPath = configuredPath.replace("~", System.getProperty("user.home"));
            Path modelFile = Paths.get(expandedPath).resolve(modelName);
            try {
                if (Files.isRegularFile(modelFile) && Files.size(modelFile) > 0) {
                    logger.debug("Using configured model path: {}", modelFile);
                    return modelFile.toAbsolutePath().toString();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 3) Check user cache directory
        try {
            Path cacheDir = getCacheDirectory(moduleSubdir);
            Path cachedModel = cacheDir.resolve(modelName);
            if (Files.exists(cachedModel) && Files.size(cachedModel) > 0) {
                logger.debug("Using cached model: {}", cachedModel);
                return cachedModel.toAbsolutePath().toString();
            }

            // 4) Auto-download if enabled
            if (autoDownload && downloadUrl != null && !downloadUrl.isBlank()) {
                return downloadModel(downloadUrl, cachedModel, modelName);
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve model from cache: {}", e.getMessage());
        }

        logger.warn("Model not found: classpathResource={}, modelName={}", classpathResource, modelName);
        return null;
    }

    /**
     * Tries to load a model from classpath resources and extract to temp directory.
     * This is required because some native libraries need filesystem paths.
     *
     * @param classpathResource the resource path (e.g., "/models/model.onnx")
     * @param modelName         the model filename
     * @param moduleSubdir      the module subdirectory
     * @return absolute path to extracted temp file, or null if not found
     */
    private static String tryLoadFromClasspath(String classpathResource, String modelName, String moduleSubdir) {
        try (InputStream is = ModelResourceLoader.class.getResourceAsStream(classpathResource)) {
            if (is == null) {
                return null;
            }

            // Extract to cache directory (not temp) to avoid repeated extractions
            Path cacheDir = getCacheDirectory(moduleSubdir);
            Files.createDirectories(cacheDir);
            Path extractedFile = cacheDir.resolve("classpath_" + modelName);

            // Only extract if not already present (optimization)
            if (!Files.exists(extractedFile) || Files.size(extractedFile) == 0) {
                Files.copy(is, extractedFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Extracted classpath model to cache: {}", extractedFile);
            }

            return extractedFile.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.debug("Failed to load from classpath: {}", classpathResource);
            return null;
        }
    }

    /**
     * Downloads a model from URL to cache directory.
     *
     * @param url        the download URL
     * @param targetPath the target file path
     * @param modelName  the model name for logging
     * @return absolute path to downloaded file
     * @throws IOException if download fails
     */
    private static String downloadModel(String url, Path targetPath, String modelName) throws IOException {
        logger.info("Downloading model: name={}, url={}", modelName, url);

        // Security: only allow HTTPS
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IOException("Only HTTPS downloads are allowed: " + url);
            }

            Files.createDirectories(targetPath.getParent());
            Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".part");

            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "spring-vision/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode + " for " + url);
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if (Files.size(tempFile) == 0) {
                Files.deleteIfExists(tempFile);
                throw new IOException("Downloaded model is empty: " + modelName);
            }

            Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Model downloaded successfully: name={}, path={}, size={} bytes",
                modelName, targetPath, Files.size(targetPath));

            return targetPath.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IOException("Failed to download model: " + modelName, e);
        }
    }

    /**
     * Gets the cache directory for a specific module.
     *
     * @param moduleSubdir the module subdirectory name
     * @return the cache directory path
     */
    private static Path getCacheDirectory(String moduleSubdir) {
        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, ".spring-vision", "models", moduleSubdir);
    }

    /**
     * Verifies SHA-256 checksum of a file.
     *
     * @param filePath         the file path
     * @param expectedChecksum the expected checksum (hex format)
     * @return true if checksum matches
     */
    public static boolean verifyChecksum(Path filePath, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return true; // No checksum to verify
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileData = Files.readAllBytes(filePath);
            byte[] hash = digest.digest(fileData);
            String actualChecksum = HexFormat.of().formatHex(hash);

            // Support both "sha256:..." and plain hex formats
            String expected = expectedChecksum.startsWith("sha256:")
                ? expectedChecksum.substring(7)
                : expectedChecksum;

            return actualChecksum.equalsIgnoreCase(expected);
        } catch (Exception e) {
            logger.warn("Failed to verify checksum for {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * Loads a resource as an InputStream from classpath.
     * Useful for XML/config files that can be used directly.
     *
     * @param classpathResource the resource path
     * @return InputStream or null if not found
     */
    public static InputStream getResourceAsStream(String classpathResource) {
        return ModelResourceLoader.class.getResourceAsStream(classpathResource);
    }

    /**
     * Checks if a classpath resource exists.
     *
     * @param classpathResource the resource path
     * @return true if resource exists
     */
    public static boolean classpathResourceExists(String classpathResource) {
        try (InputStream is = ModelResourceLoader.class.getResourceAsStream(classpathResource)) {
            return is != null;
        } catch (IOException e) {
            return false;
        }
    }
}
