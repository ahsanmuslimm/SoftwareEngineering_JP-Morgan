package com.jpmorgan.midascore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MidasCoreApplication — Spring Boot Entry Point
 *
 * This is the main application class for Midas Core — the financial
 * transaction processing service. It bootstraps the entire Spring context:
 *
 *   - @SpringBootApplication enables component scanning, auto-configuration,
 *     and Spring Boot's embedded server on port 33400.
 *
 *   - Upon startup, the following beans are automatically registered:
 *       M2: KafkaListener          — listens on "trader-updates" topic
 *       M3: TransactionService     — validates and persists transactions
 *       M3: UserRepository         — Spring Data JPA for User entities
 *       M3: TransactionRecordRepository — JPA for persisted transaction records
 *       M4: IncentiveClient        — REST calls to Incentive API
 *       M5: BalanceController      — exposes GET /balance endpoint
 *
 *   DO NOT MODIFY this class.
 */
@SpringBootApplication
public class MidasCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidasCoreApplication.class, args);
    }
}
