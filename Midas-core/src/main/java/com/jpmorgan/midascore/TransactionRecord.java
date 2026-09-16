package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * TransactionRecord — JPA Entity for persisted financial transactions.
 *
 * This is the database representation of a validated transaction.
 * It is created ONLY after a Transaction passes all validation rules:
 *   VR-01: senderId exists as a User
 *   VR-02: recipientId exists as a User
 *   VR-03: sender.balance >= transaction.amount
 *   VR-04: transaction.amount > 0 (UP-1)
 *
 * IMPORTANT: This is separate from domain/Transaction.java (the Kafka DTO).
 *            DO NOT add @Entity to Transaction.java.
 *
 * UP-1 Changes:
 *   - amount/incentive: double → BigDecimal (PH-03 — monetary precision)
 *   - transactionId: added for idempotency (PH-02 — deduplication)
 *
 * Fields:
 *   - id            : Auto-generated Long primary key
 *   - transactionId : UUID from original transaction, unique constraint for idempotency
 *   - sender        : Many-to-One relationship with User (the sending account)
 *   - recipient     : Many-to-One relationship with User (the receiving account)
 *   - amount        : The transaction amount (BigDecimal precision-safe)
 *   - incentive     : The reward amount from Incentive API (BigDecimal precision-safe)
 */
@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    /** Auto-generated surrogate primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Transaction ID from the original Kafka message, used for idempotency.
     * Unique constraint prevents duplicate processing of the same transaction.
     */
    @Column(unique = true, nullable = false)
    private String transactionId;

    /**
     * The user sending the transaction.
     * Many TransactionRecords can reference the same sender User.
     * Corresponds to User.sentTransactions (OneToMany mappedBy = "sender").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The user receiving the transaction.
     * Many TransactionRecords can reference the same recipient User.
     * Corresponds to User.receivedTransactions (OneToMany mappedBy = "recipient").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * The transaction amount (from the Kafka message).
     * Deducted from sender.balance.
     * Added to recipient.balance (plus incentive).
     * UP-1: Migrated to BigDecimal for precision safety.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * The incentive reward amount returned by the Incentive API (M4).
     * Added ONLY to recipient.balance — never deducted from sender.
     * UP-1: Migrated to BigDecimal for precision safety.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal incentive = BigDecimal.ZERO;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required by JPA */
    public TransactionRecord() {}

    public TransactionRecord(String transactionId, User sender, User recipient, BigDecimal amount, BigDecimal incentive) {
        this.transactionId = transactionId;
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getIncentive() {
        return incentive;
    }

    public void setIncentive(BigDecimal incentive) {
        this.incentive = incentive;
    }

    @Override
    public String toString() {
        return "TransactionRecord{id=" + id
                + ", transactionId='" + transactionId + "'"
                + ", sender=" + (sender != null ? sender.getId() : "null")
                + ", recipient=" + (recipient != null ? recipient.getId() : "null")
                + ", amount=" + amount
                + ", incentive=" + incentive + "}";
    }
}
