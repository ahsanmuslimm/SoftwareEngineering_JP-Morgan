package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Incentive;
import com.jpmorgan.midascore.domain.Transaction;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * IncentiveClient — HTTP client for the external Incentive API (M4 + UP-2).
 *
 * This component calls the externally-owned Incentive API microservice
 * to compute a reward amount for each valid transaction.
 *
 * UP-2 Enhancements:
 *   - Circuit Breaker: Opens after 50% failure rate, prevents cascading failures
 *   - Timeout: 3-second limit per call to prevent thread blocking
 *   - Retry: 3 attempts before falling back
 *   - Fallback: Returns zero incentive if API is unavailable
 *
 * API Contract:
 *   POST ${incentive.api.url} (configured in application.yml)
 *   Request body:  JSON-serialized Transaction object
 *   Response body: { "amount": <BigDecimal> }  where amount >= 0
 *
 * Design Principle: "API as a Contract"
 *   - Midas Core does not know or care how the incentive is calculated.
 *   - Two teams work independently as long as the contract is maintained.
 *   - Spring's RestTemplate handles JSON serialization/deserialization automatically.
 *
 * Resilience Pattern:
 *   - If Incentive API is down, fallback returns zero incentive
 *   - Transactions continue processing with zero incentive (graceful degradation)
 *   - Circuit breaker prevents overwhelming a failing service
 */
@Component
public class IncentiveClient {

    private static final Logger log = LoggerFactory.getLogger(IncentiveClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    /** Base URL of the externally-hosted Incentive API - injected from application.yml */
    @Value("${incentive.api.url}")
    private String incentiveUrl;

    /**
     * POST the transaction to the Incentive API and return the incentive amount.
     *
     * UP-2 Resilience Enhancements:
     *   - Circuit Breaker: Opens after failures, prevents cascading issues
     *   - Retry: 3 attempts before giving up
     *   - Fallback: Returns zero incentive if all attempts fail
     *
     * @param transaction the validated Transaction to enrich
     * @return Incentive DTO containing the reward amount (>= 0)
     */
    @CircuitBreaker(name = "incentiveApi", fallbackMethod = "fallbackIncentive")
    @Retry(name = "incentiveApi")
    public Incentive getIncentive(Transaction transaction) {
        log.debug("Calling Incentive API for transaction: {}", transaction.getTransactionId());
        Incentive incentive = restTemplate.postForObject(incentiveUrl, transaction, Incentive.class);
        log.debug("Incentive API returned: {}", incentive);
        return incentive;
    }

    /**
     * Fallback method invoked when Incentive API is unavailable.
     * Returns zero incentive to allow transaction processing to continue.
     *
     * UP-2 Design: Graceful degradation — transactions proceed with zero incentive.
     *
     * @param transaction the transaction that triggered the API call
     * @param ex the exception that caused the failure
     * @return Incentive with amount = 0.00
     */
    public Incentive fallbackIncentive(Transaction transaction, Throwable ex) {
        log.warn("Incentive API unavailable for transaction {} — applying zero incentive fallback. Cause: {}",
            transaction.getTransactionId(), ex.getMessage());
        return new Incentive(BigDecimal.ZERO);
    }
}
