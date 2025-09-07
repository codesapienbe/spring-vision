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
                if (expected != null && !expected.isBlank()) {
                    String actual = sha256Hex(target);
                    if (!expected.equalsIgnoreCase(actual)) {
                        Logs.warn("ModelDownloader", "checksum.mismatch_existing", Map.of("key", defaultKey));
                        Files.deleteIfExists(target);
                    } else {
                        return target.toAbsolutePath().toString();
                    }
                } else {
                    return target.toAbsolutePath().toString();
                }
            }
            downloadWithChecksum(url, target, ModelUrls.checksums().get(defaultKey));
            if (Files.size(target) == 0) throw new IllegalStateException("Downloaded model is empty: " + defaultKey);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            Logs.warn("ModelDownloader", "download.failed", Map.of("key", defaultKey, "error", e.getClass().getSimpleName()));
            return null;
        }
    }

    private static void downloadWithChecksum(String urlStr, Path target, String expectedChecksum) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
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
