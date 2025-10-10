package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Integration test that downloads a provided orchestra image and records
 * face detection metrics to `target/orchestra_detection_metrics.json`.
 *
 * <p>This test is intentionally opt-in: set the environment variable
 * `ORCHESTRA_IMAGE_URL` to run it. If the variable is not present the
 * test will be skipped.</p>
 */
public class OrchestraFalsePositiveIntegrationTest {

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024; // 50MB

    @Test
    public void runOrchestraDetectionAndRecordMetrics() throws Exception {
        String imageUrl = System.getenv("ORCHESTRA_IMAGE_URL");
        Assumptions.assumeTrue(imageUrl != null && !imageUrl.isBlank(), "ORCHESTRA_IMAGE_URL not set - skipping integration test");

        byte[] imageBytes = downloadImageBytes(imageUrl.trim());
        Assumptions.assumeTrue(imageBytes != null && imageBytes.length > 0, "Failed to download image");

        ImageData imageData = ImageData.fromBytes(imageBytes);

        VisionTemplate vt = new VisionTemplate();
        VisionResult result = vt.detectFaces(imageData);

        int count = result.detectionCount();
        double avgConfidence = result.averageConfidence();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(String.format("  \"imageUrl\": \"%s\",\n", escapeJson(imageUrl)));
        sb.append(String.format("  \"detections\": %d,\n", count));
        sb.append(String.format("  \"avgConfidence\": %.4f,\n", avgConfidence));
        sb.append("  \"items\": [\n");

        List<Detection> detections = result.detections();
        for (int i = 0; i < detections.size(); i++) {
            Detection d = detections.get(i);
            BoundingBox b = d.boundingBox();
            sb.append("    {");
            sb.append(String.format("\"label\": \"%s\", ", escapeJson(d.label())));
            sb.append(String.format("\"confidence\": %.4f, ", d.confidence()));
            sb.append(String.format("\"box\": [%.6f, %.6f, %.6f, %.6f]", b.x(), b.y(), b.width(), b.height()));
            sb.append("}");
            if (i < detections.size() - 1) sb.append(",\n");
            else sb.append("\n");
        }

        sb.append("  ]\n}");

        Path out = Path.of("target", "orchestra_detection_metrics.json");
        Files.createDirectories(out.getParent());
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private byte[] downloadImageBytes(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "spring-vision-integration-test/1.0");
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) throw new IOException("Download exceeds size limit");
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }
}


