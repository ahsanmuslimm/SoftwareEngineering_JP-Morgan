package com.jpmorgan.midascore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TransactionRecordRepository — Spring Data JPA repository for TransactionRecord entities.
 *
 * Provides CRUD operations for persisted transaction records in H2.
 * Spring Data JPA auto-generates the implementation at runtime.
 *
 * Used by:
 *   - TransactionService (M3) — to persist a new TransactionRecord after validation passes
 *   - TransactionService (UP-1) — to check for duplicate transactionId (idempotency)
 *
 * UP-1 Addition: existsByTransactionId for duplicate detection.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    
    /**
     * Check if a transaction with the given ID has already been processed.
     * Used for idempotency — prevents duplicate processing on Kafka redelivery.
     *
     * @param transactionId the unique transaction identifier
     * @return true if a record with this transactionId exists, false otherwise
     */
    boolean existsByTransactionId(String transactionId);
}
