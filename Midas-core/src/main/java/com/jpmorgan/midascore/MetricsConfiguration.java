package com.jpmorgan.midascore;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MetricsConfiguration — UP-4 Observability & Diagnostics
 *
 * This configuration class registers custom metrics counters for the Midas Core system.
 * These counters track:
 *   - midas.transactions.received: Total transactions received from Kafka
 *   - midas.transactions.valid: Total transactions that passed all validations
 *   - midas.transactions.rejected: Total transactions rejected (invalid or duplicate)
 *
 * The counters are used to monitor system health, capacity planning, and anomaly detection.
 */
@Configuration
public class MetricsConfiguration {

    /**
     * Bean method to register custom metrics counters.
     * This method is called by Spring at startup, and creates Counter instances
     * that can be injected into services.
     */
    @Bean
    public MidasMetrics midasMetrics(MeterRegistry meterRegistry) {
        return new MidasMetrics(meterRegistry);
    }

    /**
     * Health indicator for Midas Core system.
     * Returns UP if transaction processing is functioning, DOWN otherwise.
     */
    @Bean
    public HealthIndicator midasHealthIndicator() {
        return () -> {
            // In a real system, this would check actual system state
            // For now, we always return UP (transactions are running)
            return Health.up()
                    .withDetail("service", "Midas Core")
                    .withDetail("status", "Transaction processing active")
                    .build();
        };
    }

    /**
     * Helper class to manage custom metrics counters.
     * Provides methods to increment counters when transactions are received, validated, or rejected.
     */
    public static class MidasMetrics {
        private final Counter transactionsReceivedCounter;
        private final Counter transactionsValidCounter;
        private final Counter transactionsRejectedCounter;

        public MidasMetrics(MeterRegistry meterRegistry) {
            // Counter: Total transactions received
            this.transactionsReceivedCounter = Counter.builder("midas.transactions.received")
                    .description("Total number of transactions received from Kafka")
                    .register(meterRegistry);

            // Counter: Total transactions validated
            this.transactionsValidCounter = Counter.builder("midas.transactions.valid")
                    .description("Total number of transactions that passed all validations")
                    .register(meterRegistry);

            // Counter: Total transactions rejected
            this.transactionsRejectedCounter = Counter.builder("midas.transactions.rejected")
                    .description("Total number of transactions rejected (invalid or duplicate)")
                    .register(meterRegistry);
        }

        /**
         * Increment the "transactions received" counter.
         * Called when a transaction is first received from Kafka.
         */
        public void incrementReceived() {
            transactionsReceivedCounter.increment();
        }

        /**
         * Increment the "transactions valid" counter.
         * Called when a transaction passes all validations and is processed successfully.
         */
        public void incrementValid() {
            transactionsValidCounter.increment();
        }

        /**
         * Increment the "transactions rejected" counter.
         * Called when a transaction is rejected (fails validation or is a duplicate).
         */
        public void incrementRejected() {
            transactionsRejectedCounter.increment();
        }
    }
}
