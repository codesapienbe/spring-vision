package com.springvision.deepface;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for the DeepFace producer service.
 *
 * <p>This application provides a REST API for face extraction requests
 * and forwards them to Kafka for processing by Python DeepFace consumers.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication
public class DeepFaceProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepFaceProducerApplication.class, args);
    }
}
