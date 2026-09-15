package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TransactionKafkaListener — Kafka consumer for the "trader-updates" topic (M2).
 *
 * This is the ENTRY POINT of the entire Midas Core transaction pipeline.
 * It subscribes to the Kafka topic defined in application.yml, deserializes
 * incoming JSON messages into Transaction domain objects, and delegates
 * them to TransactionService for validation, enrichment, and persistence.
 *
 * Design Rules:
 *   - Topic name is injected from config via ${general.kafka-topic} — NOT hard-coded
 *   - No host/port is configured manually — tests use embedded Kafka (autowired)
 *   - The class name is TransactionKafkaListener (not KafkaListener) to avoid
 *     name collision with the Spring @KafkaListener annotation
 *
 * Data Flow:
 *   Kafka Topic → this.listen(Transaction) → TransactionService.process(Transaction)
 */
@Component
public class TransactionKafkaListener {

    private final TransactionService transactionService;

    @Autowired
    public TransactionKafkaListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Receives and processes each transaction message from the Kafka topic.
     *
     * The @KafkaListener annotation:
     *   - Subscribes to the topic name resolved from ${general.kafka-topic}
     *   - Spring Kafka deserializes the JSON payload into a Transaction object
     *   - This method is called once per message consumed
     *
     * @param transaction the deserialized Transaction from Kafka
     */
    @org.springframework.kafka.annotation.KafkaListener(
            topics = "${general.kafka-topic}"
    )
    public void listen(Transaction transaction) {
        transactionService.process(transaction);
    }
}
