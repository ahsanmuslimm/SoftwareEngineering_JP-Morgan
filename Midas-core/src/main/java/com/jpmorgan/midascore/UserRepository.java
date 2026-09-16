package com.jpmorgan.midascore;

import com.jpmorgan.midascore.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserRepository — Spring Data JPA repository for User entities.
 *
 * Provides CRUD operations for User accounts in the H2 database.
 * Spring Data JPA auto-generates the implementation at runtime.
 *
 * Used by:
 *   - TransactionService (M3) — to validate senderId/recipientId and update balances
 *   - BalanceController (M5)  — to look up a user's current balance for GET /balance
 *
 * The primary key type is String (the user's string ID, e.g. "waldorf").
 * findById(String id) is inherited from JpaRepository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // All needed methods (findById, save, findAll) are inherited from JpaRepository<User, String>
}
