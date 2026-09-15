package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.User;
import jakarta.persistence.*;

/**
 * TransactionRecord — JPA Entity for persisted financial transactions.
 *
 * This is the database representation of a validated transaction.
 * It is created ONLY after a Transaction passes all 3 validation rules:
 *   VR-01: senderId exists as a User
 *   VR-02: recipientId exists as a User
 *   VR-03: sender.balance >= transaction.amount
 *
 * IMPORTANT: This is separate from domain/Transaction.java (the Kafka DTO).
 *            DO NOT add @Entity to Transaction.java.
 *
 * Fields:
 *   - id        : Auto-generated Long primary key
 *   - sender    : Many-to-One relationship with User (the sending account)
 *   - recipient : Many-to-One relationship with User (the receiving account)
 *   - amount    : The transaction amount (as received from Kafka)
 *   - incentive : The reward amount received from the Incentive API (M4)
 *                 Defaults to 0.0 until M4 populates it.
 */
@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    /** Auto-generated surrogate primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     */
    @Column(nullable = false)
    private double amount;

    /**
     * The incentive reward amount returned by the Incentive API (M4).
     * Added ONLY to recipient.balance — never deducted from sender.
     * Defaults to 0.0 for Task 3 (before M4 is implemented).
     */
    @Column(nullable = false)
    private double incentive = 0.0;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required by JPA */
    public TransactionRecord() {}

    public TransactionRecord(User sender, User recipient, double amount, double incentive) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getIncentive() {
        return incentive;
    }

    public void setIncentive(double incentive) {
        this.incentive = incentive;
    }

    @Override
    public String toString() {
        return "TransactionRecord{id=" + id
                + ", sender=" + (sender != null ? sender.getId() : "null")
                + ", recipient=" + (recipient != null ? recipient.getId() : "null")
                + ", amount=" + amount
                + ", incentive=" + incentive + "}";
    }
}
