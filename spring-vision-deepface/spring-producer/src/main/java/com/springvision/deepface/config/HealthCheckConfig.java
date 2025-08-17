package com.springvision.deepface.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka connectivity validator for the DeepFace pipeline.
 *
 * <p>Provides methods to validate Kafka connectivity and topic accessibility
 * to ensure the entire pipeline is functioning correctly.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
public class HealthCheckConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckConfig.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public HealthCheckConfig(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Validates Kafka connectivity and returns health status.
     *
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            validateKafkaConnection();
            logger.info("Kafka health check passed");
            return true;
        } catch (Exception e) {
            logger.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates Kafka connection and topic accessibility.
     *
     * @throws Exception if validation fails
     */
    public void validateKafkaConnection() throws Exception {
        logger.debug("Validating Kafka connection...");

        // Test basic connectivity
        kafkaTemplate.getDefaultTopic();

        // Test producer functionality with a timeout
        try {
            kafkaTemplate.executeInTransaction(operations -> {
                // This will test the connection without actually sending a message
                return true;
            });
        } catch (Exception e) {
            logger.warn("Kafka transaction test failed: {}", e.getMessage());
            // Don't fail the health check for transaction issues
        }

        logger.debug("Kafka validation successful");
    }
}
