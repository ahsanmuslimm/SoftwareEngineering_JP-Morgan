package com.jpmorgan.midascore.domain;

import java.math.BigDecimal;

/**
 * Balance — DTO for the GET /balance REST endpoint response.
 *
 * When BalanceController handles GET /balance?userId=..., it returns an
 * instance of this class serialized as JSON:
 *
 *   { "userId": "waldorf", "amount": 950.0000 }
 *
 * UP-1 Change: amount migrated from double → BigDecimal (PH-03)
 *
 * CRITICAL: DO NOT modify the toString() method.
 *           The test suite (TaskFiveTests) uses toString() for verification.
 *           The exact format must be: Balance[userId=waldorf, amount=950.0000]
 *
 * Unknown user case:
 *   If userId does not exist in the database, return Balance with amount = 0.0000
 *   (200 OK response — NOT a 404).
 */
public class Balance {

    private String userId;
    private BigDecimal amount;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required for JSON serialization */
    public Balance() {}

    public Balance(String userId, BigDecimal amount) {
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
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
