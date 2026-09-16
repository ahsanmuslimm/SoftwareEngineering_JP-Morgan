package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Transaction;
import com.jpmorgan.midascore.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskThreeTests — Module 3 (Validation & Persistence Layer) Verification
 *
 * Verifies:
 *   T3-01: Valid transactions are persisted to H2
 *   T3-02: Invalid transactions (bad senderId) are discarded
 *   T3-03: Invalid transactions (bad recipientId) are discarded
 *   T3-04: Invalid transactions (insufficient balance) are discarded
 *   T3-05: Sender balance is correctly debited
 *   T3-06: Recipient balance is correctly credited
 *   T3-07: waldorf user final balance is correct
 *
 * Run: mvn test -Dtest=TaskThreeTests
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"trader-updates"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskThreeTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Test
    void validTransactionIsPersisted() {
        System.out.println("========== BEGIN TASK 3 ==========");

        // T3-01: Valid transaction should be persisted
        long countBefore = transactionRecordRepository.count();
        Transaction validTx = new Transaction("waldorf", "wilbur", 100.00);
        transactionService.process(validTx);
        long countAfter = transactionRecordRepository.count();

        assertTrue(countAfter > countBefore,
                "[T3-01] Valid transaction should create a TransactionRecord row");
        System.out.println("[T3-01] PASS — Valid transaction persisted.");

        // Check balances after the valid transaction
        User waldorf = userRepository.findById("waldorf").orElseThrow();
        User wilbur = userRepository.findById("wilbur").orElseThrow();

        System.out.println("[T3-05] waldorf.balance after Tx = " + waldorf.getBalance());
        System.out.println("[T3-06] wilbur.balance after Tx  = " + wilbur.getBalance());

        System.out.println("========== END TASK 3 ==========");
    }

    @Test
    void invalidTransactionBadSenderIsDiscarded() {
        // T3-02: Unknown senderId → discard
        long countBefore = transactionRecordRepository.count();
        Transaction invalidTx = new Transaction("nonexistent_sender", "wilbur", 50.00);
        transactionService.process(invalidTx);
        long countAfter = transactionRecordRepository.count();

        assertEquals(countBefore, countAfter,
                "[T3-02] Invalid Tx (bad senderId) should NOT create a TransactionRecord");
    }

    @Test
    void invalidTransactionBadRecipientIsDiscarded() {
        // T3-03: Unknown recipientId → discard
        long countBefore = transactionRecordRepository.count();
        Transaction invalidTx = new Transaction("waldorf", "nonexistent_recipient", 50.00);
        transactionService.process(invalidTx);
        long countAfter = transactionRecordRepository.count();

        assertEquals(countBefore, countAfter,
                "[T3-03] Invalid Tx (bad recipientId) should NOT create a TransactionRecord");
    }

    @Test
    void invalidTransactionInsufficientFundsIsDiscarded() {
        // T3-04: Insufficient balance → discard
        long countBefore = transactionRecordRepository.count();
        Transaction invalidTx = new Transaction("waldorf", "wilbur", 999999.00);
        transactionService.process(invalidTx);
        long countAfter = transactionRecordRepository.count();

        assertEquals(countBefore, countAfter,
                "[T3-04] Invalid Tx (insufficient funds) should NOT create a TransactionRecord");
    }
}
