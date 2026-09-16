package com.jpmorgan.midascore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.backoff.FixedBackOff;

/**
 * KafkaConfig — Spring Kafka configuration for error handling (UP-2).
 *
 * UP-2 Enhancement: Dead-Letter Queue (DLT) for failed messages
 *
 * This configuration sets up:
 *   1. Dead-letter topic: "trader-updates.DLT"
 *   2. Error handler: Routes failed messages to DLT after max retries
 *   3. Retry policy: 3 attempts with 1-second backoff
 *
 * Flow:
 *   - Message consumption fails
 *   - Retry 3 times (1 second apart)
 *   - After max retries, route to "trader-updates.DLT" for manual inspection
 *   - No messages are silently dropped
 */
@Configuration
public class KafkaConfig {

    /**
     * Default error handler for Kafka listener errors.
     * Routes failed messages to dead-letter topic after retries exhausted.
     *
     * UP-2: Implements graceful error handling and observability.
     *
     * @return DefaultErrorHandler with dead-letter recovery
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Create dead-letter recoverer that publishes failures to DLT
        // Topic naming convention: {originalTopic}.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate(),
            (record, ex) -> new org.springframework.kafka.retrytopic.TopicPartition(
                "trader-updates.DLT",
                record.partition()
            )
        );

        // Configure error handler with retry and backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(1000L, 3)  // 3 retries, 1 second apart
        );

        return errorHandler;
    }

    /**
     * KafkaTemplate bean required by DeadLetterPublishingRecoverer.
     * Used to send failed messages to the dead-letter topic.
     *
     * @return KafkaTemplate configured for String keys and values
     */
    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate() {
        return new org.springframework.kafka.core.KafkaTemplate<>(
            new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(
                producerConfigs()
            )
        );
    }

    /**
     * Producer configuration for dead-letter topic publishing.
     *
     * @return producer configuration map
     */
    private java.util.Map<String, Object> producerConfigs() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            org.apache.kafka.common.serialization.StringSerializer.class);
        return props;
    }
}
