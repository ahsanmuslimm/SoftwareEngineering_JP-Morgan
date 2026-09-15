package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Incentive;
import com.jpmorgan.midascore.domain.Transaction;
import com.jpmorgan.midascore.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * TransactionService — Core business logic orchestrator (M3 + M4).
 *
 * This service is the heart of Midas Core. It receives deserialized
 * Transaction objects from the KafkaListener (M2) and:
 *
 *   1. VALIDATES the transaction against 3 business rules:
 *        VR-01: senderId must match an existing User in the database
 *        VR-02: recipientId must match an existing User in the database
 *        VR-03: sender's current balance >= transaction amount
 *
 *   2. ENRICHES valid transactions by calling the Incentive API (M4):
 *        POST /incentive → receive { "amount": <double> }
 *
 *   3. UPDATES balances atomically:
 *        sender.balance    -= transaction.amount
 *        recipient.balance += transaction.amount + incentive.amount
 *
 *   4. PERSISTS the TransactionRecord to H2 via JPA
 *
 * If ANY validation rule fails, the transaction is silently DISCARDED:
 *   - No TransactionRecord row created
 *   - No balance changes
 *   - No Incentive API call
 *
 * The @Transactional annotation ensures all database writes within process()
 * are atomic — either all succeed or none are committed.
 */
@Service
public class TransactionService {

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
     * Validate → Enrich (Incentive API) → Update balances → Persist record.
     *
     * @param transaction the incoming Transaction from Kafka
     */
    @Transactional
    public void process(Transaction transaction) {

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
        if (sender.getBalance() < transaction.getAmount()) {
            // Invalid: insufficient funds → discard transaction
            return;
        }

        // ─── All validation rules passed → proceed to enrichment ────────

        // M4: Call Incentive API to get reward amount
        Incentive incentive = incentiveClient.getIncentive(transaction);
        double incentiveAmount = (incentive != null) ? incentive.getAmount() : 0.0;

        // ─── Update balances ────────────────────────────────────────────
        // Sender is debited by the transaction amount ONLY (incentive NOT deducted)
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Recipient is credited by the transaction amount PLUS the incentive
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        // Persist updated User entities
        userRepository.save(sender);
        userRepository.save(recipient);

        // ─── Persist the TransactionRecord ──────────────────────────────
        TransactionRecord record = new TransactionRecord(
                sender,
                recipient,
                transaction.getAmount(),
                incentiveAmount
        );
        transactionRecordRepository.save(record);
    }
}
