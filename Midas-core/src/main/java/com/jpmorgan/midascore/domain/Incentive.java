package com.jpmorgan.midascore.domain;

import java.math.BigDecimal;

/**
 * Incentive — DTO for the Incentive API response.
 *
 * When Midas Core POSTs a Transaction to the external Incentive API
 * (POST http://localhost:8080/incentive), the response body is deserialized
 * into this class.
 *
 * API Contract:
 *   Request:  JSON Transaction object  → { senderId, recipientId, amount }
 *   Response: JSON Incentive object    → { "amount": <BigDecimal> }
 *
 * UP-1 Change: amount migrated from double → BigDecimal (PH-03)
 *
 * Rules:
 *   - amount is always >= 0.0
 *   - This amount is added ONLY to the recipient's balance (not deducted from sender)
 *   - If amount is 0.0, no incentive is applied but no crash occurs (IT-4-04)
 */
public class Incentive {

    /** The incentive reward amount returned by the Incentive API. Always >= 0. */
    private BigDecimal amount;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required for JSON deserialization by RestTemplate */
    public Incentive() {}

    public Incentive(BigDecimal amount) {
        this.amount = amount;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Incentive{amount=" + amount + "}";
    }
}
