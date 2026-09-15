package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Incentive;
import com.jpmorgan.midascore.domain.Transaction;
import com.jpmorgan.midascore.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * TransactionService — Core business logic orchestrator (M3 + M4 + UP-1).
 *
 * This service is the heart of Midas Core. It receives deserialized
 * Transaction objects from the KafkaListener (M2) and:
 *
 *   1. IDEMPOTENCY CHECK (UP-1): Skip if transactionId already processed
 *
 *   2. VALIDATES the transaction against 4 business rules:
 *        VR-01: senderId must match an existing User in the database
 *        VR-02: recipientId must match an existing User in the database
 *        VR-03: sender's current balance >= transaction amount
 *        VR-04: transaction amount > 0 (UP-1 — no zero/negative amounts)
 *
 *   3. ENRICHES valid transactions by calling the Incentive API (M4):
 *        POST /incentive → receive { "amount": <BigDecimal> }
 *
 *   4. UPDATES balances atomically using BigDecimal precision (UP-1):
 *        sender.balance    = sender.balance.subtract(transaction.amount)
 *        recipient.balance = recipient.balance.add(transaction.amount).add(incentive.amount)
 *
 *   5. PERSISTS the TransactionRecord to H2 via JPA
 *
 * If ANY validation rule fails OR duplicate detected, the transaction is silently DISCARDED:
 *   - No TransactionRecord row created
 *   - No balance changes
 *   - No Incentive API call
 *
 * The @Transactional annotation ensures all database writes within process()
 * are atomic — either all succeed or none are committed.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final BigDecimal MAX_INCENTIVE_AMOUNT = new BigDecimal("10000.0000");

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveClient incentiveClient;

    @Autowired
    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository transactionRecordRepository,
                              IncentiveClient incentiveClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveClient = incentiveClient;
    }

    /**
     * Process a single transaction through the full pipeline:
     * Idempotency check → Validate → Enrich (Incentive API) → Update balances → Persist record.
     *
     * @param transaction the incoming Transaction from Kafka
     */
    @Transactional
    public void process(Transaction transaction) {

        // Generate transactionId if not provided (for backward compatibility with MVP tests)
        String txId = transaction.getTransactionId();
        if (txId == null || txId.isEmpty()) {
            txId = UUID.randomUUID().toString();
            transaction.setTransactionId(txId);
        }

        // ─── IDEMPOTENCY CHECK (UP-1) ───────────────────────────────────────
        if (transactionRecordRepository.existsByTransactionId(txId)) {
            // Duplicate detected — silently skip (not an error)
            return;
        }

        // ─── VR-01: senderId must exist ─────────────────────────────────
        Optional<User> senderOpt = userRepository.findById(transaction.getSenderId());
        if (senderOpt.isEmpty()) {
            // Invalid: sender does not exist → discard transaction
            return;
        }

        // ─── VR-02: recipientId must exist ──────────────────────────────
        Optional<User> recipientOpt = userRepository.findById(transaction.getRecipientId());
        if (recipientOpt.isEmpty()) {
            // Invalid: recipient does not exist → discard transaction
            return;
        }

        User sender = senderOpt.get();
        User recipient = recipientOpt.get();

        // ─── VR-03: sender balance must be >= transaction amount ────────
        if (sender.getBalance().compareTo(transaction.getAmount()) < 0) {
            // Invalid: insufficient funds → discard transaction
            return;
        }

        // ─── VR-04: transaction amount must be > 0 (UP-1) ───────────────
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // Invalid: zero or negative amount → discard transaction
            return;
        }

        // ─── All validation rules passed → proceed to enrichment ────────

        // M4 + UP-2: Call Incentive API to get reward amount (with circuit breaker)
        Incentive incentive = incentiveClient.getIncentive(transaction);
        
        // UP-2: Validate and bounds-check the incentive response
        BigDecimal incentiveAmount = validateIncentive(incentive, transaction);

        // ─── Update balances using BigDecimal precision (UP-1) ──────────
        // Sender is debited by the transaction amount ONLY (incentive NOT deducted)
        sender.setBalance(sender.getBalance().subtract(transaction.getAmount()));

        // Recipient is credited by the transaction amount PLUS the incentive
        recipient.setBalance(
            recipient.getBalance()
                .add(transaction.getAmount())
                .add(incentiveAmount)
        );

        // Persist updated User entities
        userRepository.save(sender);
        userRepository.save(recipient);

        // ─── Persist the TransactionRecord ──────────────────────────────
        TransactionRecord record = new TransactionRecord(
                txId,
                sender,
                recipient,
                transaction.getAmount(),
                incentiveAmount
        );
        transactionRecordRepository.save(record);
        
        log.info("Transaction processed successfully: id={}, sender={}, recipient={}, amount={}, incentive={}",
            txId, sender.getId(), recipient.getId(), transaction.getAmount(), incentiveAmount);
    }

    /**
     * Validate and bounds-check the incentive response from the Incentive API.
     *
     * UP-2 Safety Measures:
     *   - Null incentive → default to zero
     *   - Null amount → default to zero
     *   - Negative amount → clamp to zero
     *   - Excessive amount → cap at MAX_INCENTIVE_AMOUNT (10000.00)
     *
     * @param incentive the incentive response from the API
     * @param transaction the transaction being processed (for logging)
     * @return validated and bounds-checked incentive amount
     */
    private BigDecimal validateIncentive(Incentive incentive, Transaction transaction) {
        // Null check
        if (incentive == null || incentive.getAmount() == null) {
            log.warn("Null incentive received for transaction {} — defaulting to zero", 
                transaction.getTransactionId());
            return BigDecimal.ZERO;
        }

        BigDecimal amount = incentive.getAmount();

        // Negative check
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Negative incentive {} received for transaction {} — defaulting to zero",
                amount, transaction.getTransactionId());
            return BigDecimal.ZERO;
        }

        // Upper bound check
        if (amount.compareTo(MAX_INCENTIVE_AMOUNT) > 0) {
            log.warn("Incentive {} exceeds max bound {} for transaction {} — capping",
                amount, MAX_INCENTIVE_AMOUNT, transaction.getTransactionId());
            return MAX_INCENTIVE_AMOUNT;
        }

        return amount;
    }
}
