package com.jpmorgan.midascore.domain;

/**
 * Incentive — DTO for the Incentive API response.
 *
 * When Midas Core POSTs a Transaction to the external Incentive API
 * (POST http://localhost:8080/incentive), the response body is deserialized
 * into this class.
 *
 * API Contract:
 *   Request:  JSON Transaction object  → { senderId, recipientId, amount }
 *   Response: JSON Incentive object    → { "amount": <double> }
 *
 * Rules:
 *   - amount is always >= 0.0
 *   - This amount is added ONLY to the recipient's balance (not deducted from sender)
 *   - If amount is 0.0, no incentive is applied but no crash occurs (IT-4-04)
 */
public class Incentive {

    /** The incentive reward amount returned by the Incentive API. Always >= 0. */
    private double amount;

    // ─── Constructors ───────────────────────────────────────────────────────

    /** Required for JSON deserialization by RestTemplate */
    public Incentive() {}

    public Incentive(double amount) {
        this.amount = amount;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Incentive{amount=" + amount + "}";
    }
}
