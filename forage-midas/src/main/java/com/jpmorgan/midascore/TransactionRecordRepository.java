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
 *
 * All needed methods (save, findAll, count) are inherited from JpaRepository.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    // All needed methods are inherited from JpaRepository<TransactionRecord, Long>
}
