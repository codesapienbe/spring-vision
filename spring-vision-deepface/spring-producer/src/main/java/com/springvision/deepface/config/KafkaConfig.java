package com.springvision.deepface.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka configuration for the DeepFace producer application.
 *
 * <p>Configures Kafka producer with optimized settings for image processing
 * and includes retry logic for reliable message delivery.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Configuration
public class KafkaConfig {

    /**
     * Creates a Kafka producer factory with optimized configuration for image processing.
     *
     * @param kafkaProperties Spring Boot Kafka properties
     * @return configured producer factory
     */
    @Bean
    public ProducerFactory<String, byte[]> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.getProperties());

        // Serialization configuration
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        // Message size configuration for image processing
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 5 * 1024 * 1024); // 5MB

        // Reliability configuration
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);

        // Connection retry configuration
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Creates a Kafka template using the configured producer factory.
     *
     * @param pf the producer factory
     * @return configured Kafka template
     */
    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate(ProducerFactory<String, byte[]> pf) {
        return new KafkaTemplate<>(pf);
    }
}
