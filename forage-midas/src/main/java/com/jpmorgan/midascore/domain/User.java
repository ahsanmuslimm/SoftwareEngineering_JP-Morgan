package com.jpmorgan.midascore.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User — JPA Entity representing a Midas Core account holder.
 *
 * Each User has:
 *   - id       : String primary key (e.g. "waldorf", "wilbur")
 *   - balance  : double representing the current account balance
 *
 * Relationships:
 *   - One User can be the sender of MANY TransactionRecords
 *   - One User can be the recipient of MANY TransactionRecords
 *
 * MVP Design: Uses double for balance (simple arithmetic with +/- operators)
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
     */
    @Column(nullable = false)
    private double balance;

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

    public User(String id, double balance) {
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
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
