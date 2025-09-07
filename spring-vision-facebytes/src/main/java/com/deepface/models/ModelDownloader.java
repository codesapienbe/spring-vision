package com.deepface.models;

import com.deepface.utils.Logs;

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

public final class ModelDownloader {

    private ModelDownloader() {}

    public static String resolveOrDownload(String configuredPath, String defaultKey) {
        try {
            if (configuredPath != null && !configuredPath.isBlank()) {
                Path p = Path.of(configuredPath);
                if (Files.isRegularFile(p) && Files.size(p) > 0) return p.toAbsolutePath().toString();
            }
            Map<String, String> urls = ModelUrls.defaults();
            String url = urls.get(defaultKey);
            if (url == null || url.isBlank()) return null;
            Path cacheDir = Path.of(System.getProperty("user.home", "."), ".spring-vision", "facebytes", "models");
            Files.createDirectories(cacheDir);
            String fileName = defaultKey;
            Path target = cacheDir.resolve(fileName);
            if (Files.exists(target) && Files.size(target) > 0) {
                // If checksum is available, verify existing file
                Map<String, String> checks = ModelUrls.checksums();
                String expected = checks.get(defaultKey);
                // If no authoritative checksum provided, look for a sidecar .sha256 file in cache
                if ((expected == null || expected.isBlank())) {
                    Path sidecar = target.resolveSibling(target.getFileName().toString() + ".sha256");
                    try {
                        if (Files.exists(sidecar) && Files.size(sidecar) > 0) {
                            String sc = Files.readString(sidecar).trim();
                            if (!sc.isBlank()) expected = sc;
                        }
                    } catch (Exception ignored) { }
                }
                if (expected != null && !expected.isBlank()) {
                    String actual = sha256Hex(target);
                    if (!expected.equalsIgnoreCase(actual)) {
                        Logs.warn("ModelDownloader", "checksum.mismatch_existing", Map.of("key", defaultKey));
                        Files.deleteIfExists(target);
                        // also delete sidecar if present
                        try { Files.deleteIfExists(target.resolveSibling(target.getFileName().toString() + ".sha256")); } catch (Exception ignored) {}
                    } else {
                        return target.toAbsolutePath().toString();
                    }
                } else {
                    return target.toAbsolutePath().toString();
                }
            }

            com.deepface.config.DeepFaceConfig cfg = com.deepface.config.DeepFaceConfig.current();
            if (!cfg.autoDownloadEnabled()) {
                Logs.warn("ModelDownloader", "auto_download.disabled", Map.of("key", defaultKey));
                return null;
            }

            downloadWithChecksum(url, target, ModelUrls.checksums().get(defaultKey), cfg);
            if (Files.size(target) == 0) throw new IllegalStateException("Downloaded model is empty: " + defaultKey);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            Logs.warn("ModelDownloader", "download.failed", Map.of("key", defaultKey, "error", e.getClass().getSimpleName()));
            return null;
        }
    }

    private static void downloadWithChecksum(String urlStr, Path target, String expectedChecksum, com.deepface.config.DeepFaceConfig cfg) throws Exception {
        URL url = new URL(urlStr);
        // Security: disallow non-HTTPS by default
        if (!cfg.allowInsecureDownloads() && !"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalStateException("Insecure download protocol not allowed: " + url.getProtocol());
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(cfg.modelDownloadConnectTimeoutMs());
        conn.setReadTimeout(cfg.modelDownloadReadTimeoutMs());
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "spring-vision-facebytes/1.0");
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream raw = conn.getInputStream(); DigestInputStream in = new DigestInputStream(raw, md)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (expectedChecksum != null && !expectedChecksum.isBlank()) {
                String actual = sha256Hex(target);
                if (!expectedChecksum.equalsIgnoreCase(actual)) {
                    Logs.warn("ModelDownloader", "checksum.mismatch", Map.of("url", urlStr));
                    Files.deleteIfExists(target);
                    throw new IllegalStateException("Checksum verification failed for " + urlStr);
                }
                // write authoritative checksum alongside artifact
                try { Files.writeString(target.resolveSibling(target.getFileName().toString() + ".sha256"), expectedChecksum); } catch (Exception ignored) {}
            } else {
                // No authoritative checksum: compute and write sidecar for future verification
                try {
                    String actual = sha256Hex(target);
                    Files.writeString(target.resolveSibling(target.getFileName().toString() + ".sha256"), actual);
                } catch (Exception ignored) {}
            }
        } else {
            throw new IllegalStateException("HTTP " + code + " for " + urlStr);
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
