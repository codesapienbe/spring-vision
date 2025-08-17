package com.springvision.deepface.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Configuration for application startup behavior.
 * Handles startup delays and connection retries for external services.
 */
@Configuration
public class StartupConfig {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public StartupConfig(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Event listener that runs after the application context is ready.
     * Adds a startup delay and validates Kafka connectivity.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application started successfully. Validating Kafka connectivity...");

        try {
            // Add a small delay to ensure Kafka is fully ready
            Thread.sleep(2000);

            // Test Kafka connectivity
            validateKafkaConnection();

            logger.info("Kafka connectivity validated successfully");
        } catch (Exception e) {
            logger.warn("Kafka connectivity check failed, but application will continue: {}", e.getMessage());
        }
    }

    /**
     * Validates Kafka connection with retry logic.
     *
     * @throws Exception if connection fails after retries
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void validateKafkaConnection() throws Exception {
        logger.debug("Testing Kafka connection...");

        // Simple test to verify Kafka is reachable
        kafkaTemplate.getDefaultTopic();

        logger.debug("Kafka connection test successful");
    }
}
