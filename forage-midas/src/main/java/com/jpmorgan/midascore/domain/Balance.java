package com.jpmorgan.midascore.domain;

/**
 * Balance — DTO for the GET /balance REST endpoint response.
 *
 * When BalanceController handles GET /balance?userId=..., it returns an
 * instance of this class serialized as JSON:
 *
 *   { "userId": "waldorf", "amount": 950.00 }
 *
 * CRITICAL: DO NOT modify the toString() method.
 *           The test suite (TaskFiveTests) uses toString() for verification.
 *           The exact format must be: Balance[userId=waldorf, amount=950.0]
 *
 * Unknown user case:
 *   If userId does not exist in the database, return Balance with amount = 0.0
 *   (200 OK response — NOT a 404).
 */
public class Balance {

    private String userId;
    private double amount;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required for JSON serialization */
    public Balance() {}

    public Balance(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * DO NOT MODIFY — Used by TaskFiveTests for automated verification.
     * Format: Balance[userId=<id>, amount=<amount>]
     */
    @Override
    public String toString() {
        return "Balance[userId=" + userId + ", amount=" + amount + "]";
    }
}
