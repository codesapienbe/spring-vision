package com.springvision.deepface.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FaceExtractionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceExtractionController.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topic.face-tasks}")
    private String topic;

    @PostMapping(value = "/extract-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> extractFace(
            @RequestParam("userId") String userId,
            @RequestParam("file") MultipartFile file) {

        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // Validate input
            if (userId == null || userId.trim().isEmpty()) {
                logger.warn("Invalid userId provided", Map.of(
                    "correlation_id", correlationId,
                    "userId", userId
                ));
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            if (file == null || file.isEmpty()) {
                logger.warn("No file provided", Map.of(
                    "correlation_id", correlationId,
                    "userId", userId
                ));
                return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
            }

            // Process image
            byte[] imageBytes = file.getBytes();
            String kafkaKey = userId + ":" + file.getOriginalFilename();

            logger.info("Sending face extraction request", Map.of(
                "correlation_id", correlationId,
                "userId", userId,
                "filename", file.getOriginalFilename(),
                "fileSize", imageBytes.length,
                "topic", topic
            ));

            kafkaTemplate.send(topic, kafkaKey, imageBytes);

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Face extraction request queued successfully", Map.of(
                "correlation_id", correlationId,
                "userId", userId,
                "filename", file.getOriginalFilename(),
                "processingTimeMs", processingTime
            ));

            Map<String, String> response = new HashMap<>();
            response.put("status", "queued");
            response.put("filename", file.getOriginalFilename());
            response.put("correlationId", correlationId);
            response.put("processingTimeMs", String.valueOf(processingTime));

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to process face extraction request", Map.of(
                "correlation_id", correlationId,
                "userId", userId,
                "filename", file != null ? file.getOriginalFilename() : "null",
                "error", e.getMessage(),
                "processingTimeMs", processingTime
            ), e);

            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to process request",
                "correlationId", correlationId
            ));
        }
    }
}
