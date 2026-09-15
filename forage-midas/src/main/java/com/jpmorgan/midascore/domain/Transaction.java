package com.jpmorgan.midascore.domain;

import java.math.BigDecimal;

/**
 * Transaction — Incoming Kafka message DTO.
 *
 * This class represents a financial transaction published by a producer to
 * the "trader-updates" Kafka topic. It is deserialized from JSON by the
 * KafkaListener and passed through the processing pipeline.
 *
 * IMPORTANT: DO NOT add @Entity to this class. It is a DTO only.
 *            The persisted form is TransactionRecord.
 *
 * UP-1 Changes:
 *   - amount: double → BigDecimal (PH-03 — monetary precision)
 *   - transactionId: added for idempotency (PH-02 — deduplication on Kafka redelivery)
 *
 * Fields:
 *   - transactionId : UUID assigned by the producer, used for idempotency
 *   - senderId      : ID of the user initiating the transaction
 *   - recipientId   : ID of the user receiving the transaction
 *   - amount        : The transaction amount (BigDecimal — precision safe)
 */
public class Transaction {

    private String transactionId;
    private String senderId;
    private String recipientId;
    private BigDecimal amount;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required for JSON deserialization */
    public Transaction() {}

    public Transaction(String senderId, String recipientId, BigDecimal amount) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
    }

    public Transaction(String transactionId, String senderId, String recipientId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction{transactionId='" + transactionId
                + "', senderId='" + senderId
                + "', recipientId='" + recipientId
                + "', amount=" + amount + "}";
    }
}
