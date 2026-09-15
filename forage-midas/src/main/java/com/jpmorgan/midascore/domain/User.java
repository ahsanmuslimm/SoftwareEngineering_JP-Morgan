package com.jpmorgan.midascore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * User — JPA Entity representing a Midas Core account holder.
 *
 * Each User has:
 *   - id       : String primary key (e.g. "waldorf", "wilbur")
 *   - balance  : BigDecimal representing the current account balance
 *
 * Relationships:
 *   - One User can be the sender of MANY TransactionRecords
 *   - One User can be the recipient of MANY TransactionRecords
 *
 * UP-1 Change: balance migrated from double → BigDecimal (PH-03)
 *   - @Column(precision = 19, scale = 4) ensures proper DB storage
 *   - All arithmetic must use BigDecimal.add()/subtract() — NOT +/- operators
 *
 * NOTE: The table is named "users" (not "user") because USER is a reserved
 *       keyword in H2 and most SQL databases.
 */
@Entity
@Table(name = "users")
public class User {

    /** String primary key — matches the user ID used in Transaction messages. */
    @Id
    private String id;

    /**
     * Current account balance. Updated atomically during transaction processing.
     * precision = 19 digits total, scale = 4 decimal places.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /**
     * Transactions where this user is the SENDER.
     * mappedBy = "sender" refers to the field name in TransactionRecord.
     */
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionRecord> sentTransactions = new ArrayList<>();

    /**
     * Transactions where this user is the RECIPIENT.
     * mappedBy = "recipient" refers to the field name in TransactionRecord.
     */
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionRecord> receivedTransactions = new ArrayList<>();

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required by JPA */
    public User() {}

    public User(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<TransactionRecord> getSentTransactions() {
        return sentTransactions;
    }

    public List<TransactionRecord> getReceivedTransactions() {
        return receivedTransactions;
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', balance=" + balance + "}";
    }
}
