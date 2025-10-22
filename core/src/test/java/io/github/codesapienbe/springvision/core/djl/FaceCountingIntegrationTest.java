package io.github.codesapienbe.springvision.core.djl;

import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for face counting functionality in the core module.
 */
public class FaceCountingIntegrationTest {

    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    @Test
    public void testFaceCountingFromUrl() throws Exception {
        // Create Spring context with auto-configuration
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(VisionAutoConfiguration.class);
            context.refresh();

            VisionTemplate visionTemplate = context.getBean(VisionTemplate.class);

            // Given
            String imageUrl = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg";

            // When
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            ImageData imageData = ImageData.fromBytes(imageBytes);
            VisionResult result = visionTemplate.detectFaces(imageData);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.detectionCount()).isGreaterThanOrEqualTo(0);

            System.out.println("Face counting test completed:");
            System.out.println("- Image URL: " + imageUrl);
            System.out.println("- Image size: " + imageBytes.length + " bytes");
            System.out.println("- Detected faces: " + result.detectionCount());
            System.out.println("- Average confidence: " + result.averageConfidence());
        }
    }

    private byte[] downloadImageFromUrl(String imageUrl) throws IOException, InterruptedException {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IOException("Image URL is required");
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(imageUrl))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "SpringVision-Test/1.0")
            .header("Accept", "image/*, */*")
            .GET()
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP error " + response.statusCode());
        }

        byte[] imageBytes = response.body();
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
        }

        return imageBytes;
    }
}
