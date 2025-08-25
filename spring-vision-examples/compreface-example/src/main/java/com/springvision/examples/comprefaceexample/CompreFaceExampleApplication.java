package com.springvision.examples.comprefaceexample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;

/**
 * Example application demonstrating CompreFace backend usage.
 *
 * <p>This application shows how to use the CompreFace backend to detect faces
 * and perform face recognition using a containerized CompreFace API.</p>
 *
 * <p>It also supports HTTP/HTTPS image URLs as input with SSRF-safe in-memory
 * downloading (public host validation, size/time limits).</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication
public class CompreFaceExampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(CompreFaceExampleApplication.class);

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024; // 50MB

    public static void main(String[] args) {
        SpringApplication.run(CompreFaceExampleApplication.class, args);
    }

    @Bean
    public CommandLineRunner compreFaceExample(VisionTemplate visionTemplate) {
        return args -> {
            logger.info("=== CompreFace Example Application ===");

            // Check if CompreFace backend is available
            if (!"compreface".equals(visionTemplate.getBackendId())) {
                logger.warn("CompreFace backend is not configured. Current backend: {}", visionTemplate.getBackendId());
                logger.info("To use CompreFace backend, configure vision.backend=compreface in application.yml");
                return;
            }

            logger.info("Using CompreFace backend: {}", visionTemplate.getBackendDisplayName());

            // Check backend health
            if (!visionTemplate.isBackendHealthy()) {
                logger.error("CompreFace backend is not healthy: {}",
                    visionTemplate.getBackendHealthInfo().errorMessage());
                return;
            }

            logger.info("CompreFace backend is healthy and ready");

            // Example: Load and process an image
            if (args.length > 0) {
                String imagePath = args[0];
                processImage(visionTemplate, imagePath);
            } else {
                logger.info("No image path provided. Usage: java -jar compreface-example.jar <image-path-or-URL>");
                logger.info("Example: java -jar compreface-example.jar /path/to/face.jpg");
                logger.info("Example: java -jar compreface-example.jar https://example.com/face.jpg");
            }
        };
    }

    private void processImage(VisionTemplate visionTemplate, String imageSource) {
        try {
            logger.info("Processing image: {}", imageSource);

            // Load image
            byte[] imageBytes = isHttpUrl(imageSource)
                    ? downloadImageBytes(imageSource)
                    : Files.readAllBytes(Path.of(imageSource));
            ImageData imageData = ImageData.fromBytes(imageBytes);

            logger.info("Image loaded: {} bytes, format: {}",
                imageData.size(), imageData.format());

            // Detect faces
            logger.info("Detecting faces...");
            com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
                .type(com.springvision.core.DetectionType.FACE)
                .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
                .build();
            VisionResult result = visionTemplate.detect(imageData, query);

            if (result.hasDetections()) {
                logger.info("Found {} faces:", result.detectionCount());
                result.detections().forEach(detection -> {
                    logger.info("  - Face at {} with confidence: {:.2f}",
                        detection.boundingBox(), detection.confidence());

                    // Show CompreFace-specific attributes if available
                    if (detection.hasAttribute("age")) {
                        logger.info("    Age: {}", detection.getAttribute("age"));
                    }
                    if (detection.hasAttribute("gender")) {
                        logger.info("    Gender: {}", detection.getAttribute("gender"));
                    }
                    if (detection.hasAttribute("mask")) {
                        logger.info("    Mask: {}", detection.getAttribute("mask"));
                    }
                });
            } else {
                logger.info("No faces detected in the image");
            }

            logger.info("Processing completed in {}ms", result.processingTimeMs());

        } catch (Exception e) {
            logger.error("Failed to process image: {}", e.getMessage(), e);
        }
    }

    private boolean isHttpUrl(String s) {
        String v = s == null ? "" : s.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private byte[] downloadImageBytes(String urlString) throws Exception {
        URL url = URI.create(urlString).toURL();
        validatePublicHost(url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "spring-vision-compreface-example/1.0");
        String ct = conn.getContentType();
        if (ct != null && !ct.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("URL does not point to an image content type");
        }
        int contentLength = conn.getContentLength();
        if (contentLength > 0 && contentLength > MAX_DOWNLOAD_BYTES) {
            throw new IllegalArgumentException("Remote content too large");
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read; int total = 0;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IllegalArgumentException("Download exceeds size limit");
                }
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private void validatePublicHost(URL url) throws Exception {
        String host = url.getHost();
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Invalid URL host");
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve host: " + host);
        }
        for (InetAddress a : addrs) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Refusing to connect to non-public address: " + a.getHostAddress());
            }
        }
    }
}
