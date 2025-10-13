package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Utility class for downloading and managing ONNX models.
 */
public final class ModelDownloader {

    private ModelDownloader() {
    }

    /**
     * Resolve an on-disk model path or attempt a secure download into the local cache.
     * Returns absolute path to the model file or null when not available (or disabled).
     * @param configuredPath The path to the model file configured by the user.
     * @param defaultKey The key to look up the default URL for the model.
     * @return The absolute path to the model file, or null if not available.
     */
    public static String resolveOrDownload(String configuredPath, String defaultKey) {
        try {
            // 1) Respect explicitly configured path if valid
            if (configuredPath != null && !configuredPath.isBlank()) {
                Path p = Path.of(configuredPath);
                if (Files.isRegularFile(p) && Files.size(p) > 0) return p.toAbsolutePath().toString();
                Logs.warn("ModelDownloader", "configured_path_invalid", Map.of("path", configuredPath));
            }

            // 2) Lookup default URL for the requested model
            Map<String, String> urls = ModelUrls.defaults();
            String url = urls.get(defaultKey);
            if (url == null || url.isBlank()) {
                Logs.warn("ModelDownloader", "no_default_url", Map.of("key", defaultKey));
                return null;
            }

            // 3) Prepare cache directory and target path
            Path cacheDir = Path.of(System.getProperty("user.home", "."), ".spring-vision", "facebytes", "models");
            Files.createDirectories(cacheDir);
            String fileName = defaultKey;
            Path target = cacheDir.resolve(fileName);

            // 4) Validate existing cached file (checksum/sidecar)
            if (Files.exists(target) && Files.size(target) > 0) {
                Map<String, String> checks = ModelUrls.checksums();
                String expected = checks.get(defaultKey);
                if (expected == null || expected.isBlank()) {
                    // Look for sidecar checksum
                    Path sidecar = target.resolveSibling(target.getFileName().toString() + ".sha256");
                    try {
                        if (Files.exists(sidecar) && Files.size(sidecar) > 0) {
                            String sc = Files.readString(sidecar).trim();
                            if (!sc.isBlank()) expected = sc;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (expected != null && !expected.isBlank()) {
                    String actual = sha256Hex(target);
                    if (!expected.equalsIgnoreCase(actual)) {
                        Logs.warn("ModelDownloader", "checksum.mismatch_existing", Map.of("key", defaultKey));
                        Files.deleteIfExists(target);
                        try {
                            Files.deleteIfExists(target.resolveSibling(target.getFileName().toString() + ".sha256"));
                        } catch (Exception ignored) {
                        }
                    } else {
                        Logs.info("ModelDownloader", "cache.valid", Map.of("key", defaultKey, "path", target.toAbsolutePath().toString()));
                        return target.toAbsolutePath().toString();
                    }
                } else {
                    Logs.info("ModelDownloader", "cache.no_checksum", Map.of("key", defaultKey, "path", target.toAbsolutePath().toString()));
                    return target.toAbsolutePath().toString();
                }
            }

            // 5) Auto-download guard
            io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig cfg = io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig.current();
            if (!cfg.autoDownloadEnabled()) {
                Logs.warn("ModelDownloader", "auto_download.disabled", Map.of("key", defaultKey));
                return null;
            }

            // 6) Attempt download with retries
            String expectedChecksum = ModelUrls.checksums().get(defaultKey);
            downloadWithChecksumWithRetries(url, target, expectedChecksum, cfg, 3);

            if (!Files.exists(target) || Files.size(target) == 0)
                throw new IllegalStateException("Downloaded model is empty: " + defaultKey);
            Logs.info("ModelDownloader", "download.complete", Map.of("key", defaultKey, "path", target.toAbsolutePath().toString()));
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            Logs.warn("ModelDownloader", "download.failed", Map.of("key", defaultKey, "error", e.getClass().getSimpleName(), "message", e.getMessage()));
            return null;
        }
    }

    private static void downloadWithChecksumWithRetries(String urlStr, Path target, String expectedChecksum, io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig cfg, int maxAttempts) throws Exception {
        Exception last = null;
        long backoffMs = 1000L;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                downloadWithChecksum(urlStr, target, expectedChecksum, cfg);
                return;
            } catch (Exception e) {
                last = e;
                Logs.warn("ModelDownloader", "download.attempt_failed", Map.of("url", urlStr, "attempt", attempt, "error", e.getClass().getSimpleName()));
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    backoffMs *= 2L;
                }
            }
        }
        throw last;
    }

    private static void downloadWithChecksum(String urlStr, Path target, String expectedChecksum, io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig cfg) throws Exception {
        URL url = new URL(urlStr);
        // Security: disallow non-HTTPS by default
        if (!cfg.allowInsecureDownloads() && !"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalStateException("Insecure download protocol not allowed: " + url.getProtocol());
        }

        Path temp = target.resolveSibling(target.getFileName().toString() + ".part");
        Files.deleteIfExists(temp);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(cfg.modelDownloadConnectTimeoutMs());
        conn.setReadTimeout(cfg.modelDownloadReadTimeoutMs());
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "spring-vision-facebytes/1.0");
        conn.setRequestProperty("Accept", "application/octet-stream");

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " for " + urlStr);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream raw = conn.getInputStream(); DigestInputStream in = new DigestInputStream(raw, md)) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }

        // Ensure non-empty
        if (!Files.exists(temp) || Files.size(temp) == 0) {
            Files.deleteIfExists(temp);
            throw new IllegalStateException("Downloaded model file is empty");
        }

        // Checksum verification
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual = sha256Hex(temp);
            if (!expectedChecksum.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(temp);
                Logs.warn("ModelDownloader", "checksum.mismatch", Map.of("url", urlStr, "expected", expectedChecksum, "actual", actual));
                throw new IllegalStateException("Checksum verification failed for " + urlStr);
            }
            // write authoritative checksum alongside artifact
            try {
                Files.writeString(temp.resolveSibling(temp.getFileName().toString() + ".sha256"), expectedChecksum);
            } catch (Exception ignored) {
            }
        } else {
            // No authoritative checksum: compute and write sidecar for future verification
            try {
                String actual = sha256Hex(temp);
                try {
                    Files.writeString(temp.resolveSibling(temp.getFileName().toString() + ".sha256"), actual);
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }
        }

        // Atomic move into place when possible
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            // Fallback to non-atomic move
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256Hex(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(p); DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) { /* read to update digest */ }
        }
        byte[] digest = md.digest();
        return HexFormat.of().formatHex(digest);
    }
}
