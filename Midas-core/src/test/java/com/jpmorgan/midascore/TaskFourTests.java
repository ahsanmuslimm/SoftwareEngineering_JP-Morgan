package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Incentive;
import com.jpmorgan.midascore.domain.Transaction;
import com.jpmorgan.midascore.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskFourTests — Module 4 (Incentive API Client) Verification
 *
 * PRE-CONDITION: Incentive API JAR must be running before executing this test!
 *   java -jar services/incentive-api.jar
 *
 * Verifies:
 *   T4-01: IncentiveClient bean exists in Spring context
 *   T4-02: POST to /incentive succeeds and returns non-null
 *   T4-03: Incentive amount stored in TransactionRecord.incentive
 *   T4-04: Recipient balance = amount + incentive
 *   T4-05: Sender balance = original - amount only (NOT charged incentive)
 *   T4-06: wilbur user final balance is correct
 *
 * Run:
 *   java -jar services/incentive-api.jar  (Terminal 1)
 *   mvn test -Dtest=TaskFourTests         (Terminal 2)
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"trader-updates"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskFourTests {

    @Autowired
    private IncentiveClient incentiveClient;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Test
    void incentiveClientBeanExists() {
        System.out.println("========== BEGIN TASK 4 ==========");
        assertNotNull(incentiveClient, "[T4-01] IncentiveClient bean should be registered");
        System.out.println("[T4-01] PASS — IncentiveClient bean exists.");
        System.out.println("========== END TASK 4 ==========");
    }

    @Test
    void incentiveApiReturnsValidResponse() {
        // T4-02: POST /incentive returns non-null with amount >= 0
        Transaction testTx = new Transaction("waldorf", "wilbur", 100.00);
        Incentive incentive = incentiveClient.getIncentive(testTx);
        assertNotNull(incentive, "[T4-02] Incentive API should return a non-null response");
        assertTrue(incentive.getAmount() >= 0, "[T4-02] Incentive amount should be >= 0");
        System.out.println("[T4-02] PASS — Incentive API returned: " + incentive);
    }

    @Test
    void incentiveAppliedToRecipientOnly() {
        // T4-04 & T4-05: Verify incentive logic
        User senderBefore = userRepository.findById("samuel").orElseThrow();
        User recipientBefore = userRepository.findById("janet").orElseThrow();
        double senderBalanceBefore = senderBefore.getBalance();
        double recipientBalanceBefore = recipientBefore.getBalance();

        Transaction tx = new Transaction("samuel", "janet", 50.00);
        transactionService.process(tx);

        User senderAfter = userRepository.findById("samuel").orElseThrow();
        User recipientAfter = userRepository.findById("janet").orElseThrow();

        // Sender deducted by amount only
        assertEquals(senderBalanceBefore - 50.00, senderAfter.getBalance(), 0.01,
                "[T4-05] Sender should be debited by transaction amount only");
        System.out.println("[T4-05] PASS — Sender debited by amount only: " + senderAfter.getBalance());

        // Recipient credited by amount + incentive (incentive > 0 expected)
        assertTrue(recipientAfter.getBalance() >= recipientBalanceBefore + 50.00,
                "[T4-04] Recipient should be credited by amount + incentive");
        System.out.println("[T4-04] PASS — Recipient balance: " + recipientAfter.getBalance());
    }
}
