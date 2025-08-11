package com.deepface.models;

import com.deepface.utils.Logs;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
                return target.toAbsolutePath().toString();
            }
            download(url, target);
            if (Files.size(target) == 0) throw new IllegalStateException("Downloaded model is empty: " + defaultKey);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            Logs.warn("ModelDownloader", "download.failed", Map.of("key", defaultKey, "error", e.getClass().getSimpleName()));
            return null;
        }
    }

    private static void download(String urlStr, Path target) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "spring-vision-facebytes/1.0");
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new IllegalStateException("HTTP " + code + " for " + urlStr);
        }
    }
}
