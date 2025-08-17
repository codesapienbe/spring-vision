package com.springvision.examples.deepfaceexample;

import java.nio.file.Files;
import java.nio.file.Path;

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
 * Example application demonstrating DeepFace backend usage.
 *
 * <p>This application shows how to use the DeepFace backend to detect faces
 * and perform face analysis using a containerized DeepFace API.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication
public class DeepFaceExampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(DeepFaceExampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DeepFaceExampleApplication.class, args);
    }

    @Bean
    public CommandLineRunner deepFaceExample(VisionTemplate visionTemplate) {
        return args -> {
            logger.info("=== DeepFace Example Application ===");

            // Check if DeepFace backend is available
            if (!"deepface".equals(visionTemplate.getBackendId())) {
                logger.warn("DeepFace backend is not configured. Current backend: {}", visionTemplate.getBackendId());
                logger.info("To use DeepFace backend, configure vision.backend=deepface in application.yml");
                return;
            }

            logger.info("Using DeepFace backend: {}", visionTemplate.getBackendDisplayName());

            // Check backend health
            if (!visionTemplate.isBackendHealthy()) {
                logger.error("DeepFace backend is not healthy: {}",
                    visionTemplate.getBackendHealthInfo().errorMessage());
                return;
            }

            logger.info("DeepFace backend is healthy and ready");

            // Example: Load and process an image
            if (args.length > 0) {
                String imagePath = args[0];
                processImage(visionTemplate, imagePath);
            } else {
                logger.info("No image path provided. Usage: java -jar deepface-example.jar <image-path>");
                logger.info("Example: java -jar deepface-example.jar /path/to/face.jpg");
            }
        };
    }

    private void processImage(VisionTemplate visionTemplate, String imagePath) {
        try {
            logger.info("Processing image: {}", imagePath);

            // Load image
            byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
            ImageData imageData = ImageData.fromBytes(imageBytes);

            logger.info("Image loaded: {} bytes, format: {}",
                imageData.size(), imageData.format());

            // Detect faces
            logger.info("Detecting faces...");
            VisionResult result = visionTemplate.detectFaces(imageData);

            if (result.hasDetections()) {
                logger.info("Found {} faces:", result.detectionCount());
                result.detections().forEach(detection -> {
                    logger.info("  - Face at {} with confidence: {:.2f}",
                        detection.boundingBox(), detection.confidence());

                    // Show DeepFace-specific attributes if available
                    if (detection.hasAttribute("age")) {
                        logger.info("    Age: {}", detection.getAttribute("age"));
                    }
                    if (detection.hasAttribute("gender")) {
                        logger.info("    Gender: {}", detection.getAttribute("gender"));
                    }
                    if (detection.hasAttribute("emotion")) {
                        logger.info("    Emotion: {}", detection.getAttribute("emotion"));
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
}
