package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.Incentive;
import com.jpmorgan.midascore.domain.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * IncentiveClient — HTTP client for the external Incentive API (M4).
 *
 * This component calls the externally-owned Incentive API microservice
 * to compute a reward amount for each valid transaction.
 *
 * API Contract:
 *   POST http://localhost:8080/incentive
 *   Request body:  JSON-serialized Transaction object
 *   Response body: { "amount": <double> }  where amount >= 0
 *
 * Design Principle: "API as a Contract"
 *   - Midas Core does not know or care how the incentive is calculated.
 *   - Two teams work independently as long as the contract is maintained.
 *   - Spring's RestTemplate handles JSON serialization/deserialization automatically.
 *
 * Pre-condition:
 *   The Incentive API JAR must be running before calling this client:
 *     java -jar services/incentive-api.jar
 *
 * If the Incentive API is not running, a ResourceAccessException will be thrown.
 */
@Component
public class IncentiveClient {

    private final RestTemplate restTemplate = new RestTemplate();

    /** Base URL of the externally-hosted Incentive API. */
    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";

    /**
     * POST the transaction to the Incentive API and return the incentive amount.
     *
     * @param transaction the validated Transaction to enrich
     * @return Incentive DTO containing the reward amount (>= 0)
     */
    public Incentive getIncentive(Transaction transaction) {
        return restTemplate.postForObject(INCENTIVE_URL, transaction, Incentive.class);
    }
}
