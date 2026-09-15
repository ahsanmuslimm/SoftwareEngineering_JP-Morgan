package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Balance;
import com.jpmorgan.midascore.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * BalanceController — REST endpoint for querying user balances (M5).
 *
 * This is the OUTPUT LAYER of Midas Core. It exposes a simple HTTP GET
 * endpoint that allows external consumers to query a user's current
 * account balance. It completes the full end-to-end pipeline:
 *
 *   Kafka → Validation → Incentive → Persistence → REST Query (here)
 *
 * Endpoint:
 *   GET http://localhost:33400/balance?userId={userId}
 *
 * Response (200 OK):
 *   { "userId": "waldorf", "amount": 950.0 }
 *
 * Unknown user (200 OK — by design, NOT a 404):
 *   { "userId": "unknown", "amount": 0.0 }
 *
 * Architectural Decision:
 *   The balance endpoint lives inside Midas Core (same Spring Boot process)
 *   rather than a separate microservice. This was chosen for simplicity at
 *   current scale — if the balance API grows, it can be extracted later.
 *
 * CRITICAL RULES:
 *   - Do NOT change the server port from 33400
 *   - Do NOT modify Balance.toString() — test suite uses it for verification
 *   - The REST controller runs ALONGSIDE the Kafka listener in the same process
 */
@RestController
public class BalanceController {

    private final UserRepository userRepository;

    @Autowired
    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * GET /balance?userId={userId}
     *
     * Looks up the user in the H2 database via UserRepository:
     *   - If found: returns Balance with the user's current balance
     *   - If not found: returns Balance with amount = 0.0 (200 OK, not 404)
     *
     * @param userId the user ID to look up
     * @return Balance DTO serialized as JSON
     */
    @GetMapping("/balance")
    public Balance getBalance(@RequestParam String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt
                .map(user -> new Balance(userId, user.getBalance()))
                .orElse(new Balance(userId, 0.0));
    }
}
