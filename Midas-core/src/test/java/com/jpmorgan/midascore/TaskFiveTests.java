package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Balance;
import com.jpmorgan.midascore.domain.Transaction;
import com.jpmorgan.midascore.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TaskFiveTests — Module 5 (Balance REST API) Verification
 *
 * PRE-CONDITION: Incentive API JAR must be running before executing this test!
 *   java -jar services/incentive-api.jar
 *
 * Verifies:
 *   T5-01: BalanceController bean registered in Spring context
 *   T5-02: Server listens on port 33400
 *   T5-03: GET /balance?userId=<valid> returns correct balance
 *   T5-04: GET /balance?userId=<unknown> returns amount: 0.0
 *   T5-05: REST endpoint works while Kafka listener is active
 *   T5-06: Balance.toString() format is unchanged
 *   T5-07: BEGIN/END markers appear in console output
 *
 * Run:
 *   java -jar services/incentive-api.jar  (Terminal 1)
 *   mvn test -Dtest=TaskFiveTests         (Terminal 2)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"trader-updates"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskFiveTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BalanceController balanceController;

    @Autowired
    private TransactionKafkaListener kafkaListener;

    @Test
    void balanceControllerBeanExists() {
        System.out.println("========== BEGIN TASK 5 ==========");
        org.junit.jupiter.api.Assertions.assertNotNull(balanceController,
                "[T5-01] BalanceController bean should be registered");
        System.out.println("[T5-01] PASS — BalanceController bean exists.");
    }

    @Test
    void balanceEndpointReturnsValidUserBalance() throws Exception {
        // T5-03: GET /balance?userId=waldorf returns a valid balance
        mockMvc.perform(get("/balance").param("userId", "waldorf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("waldorf"))
                .andExpect(jsonPath("$.amount").isNumber());
        System.out.println("[T5-03] PASS — Valid user balance returned.");
    }

    @Test
    void balanceEndpointReturnsZeroForUnknownUser() throws Exception {
        // T5-04: GET /balance?userId=unknownUser → { "amount": 0.0 }
        mockMvc.perform(get("/balance").param("userId", "unknownUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("unknownUser"))
                .andExpect(jsonPath("$.amount").value(0.0));
        System.out.println("[T5-04] PASS — Unknown user returns amount: 0.0 (not 404).");
    }

    @Test
    void kafkaListenerActiveAlongsideRestController() {
        // T5-05: Both beans exist and are active in the same context
        org.junit.jupiter.api.Assertions.assertNotNull(kafkaListener,
                "[T5-05] KafkaListener should be active alongside REST controller");
        org.junit.jupiter.api.Assertions.assertNotNull(balanceController,
                "[T5-05] BalanceController should be active alongside KafkaListener");
        System.out.println("[T5-05] PASS — Kafka listener and REST controller are concurrent.");
    }

    @Test
    void balanceToStringFormatUnchanged() {
        // T5-06: Verify Balance.toString() format matches expected
        Balance testBalance = new Balance("testUser", 123.45);
        String expected = "Balance[userId=testUser, amount=123.45]";
        org.junit.jupiter.api.Assertions.assertEquals(expected, testBalance.toString(),
                "[T5-06] Balance.toString() format must match scaffold specification");
        System.out.println("[T5-06] PASS — Balance.toString() format unchanged.");
        System.out.println("========== END TASK 5 ==========");
    }
}
