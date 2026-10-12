package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.RollbackIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RollbackIntentRepository extends JpaRepository<RollbackIntent, Long> {

    /**
     * Find rollback intent by provider and rollback transaction ID.
     */
    Optional<RollbackIntent> findByProviderIdAndRollbackTransactionId(Long providerId, String rollbackTransactionId);

    /**
     * Check if rollback intent exists.
     */
    boolean existsByProviderIdAndRollbackTransactionId(Long providerId, String rollbackTransactionId);

    /**
     * Find all rollback intents waiting for a specific original transaction.
     */
    List<RollbackIntent> findByProviderIdAndOriginalTransactionId(Long providerId, String originalTransactionId);

    /**
     * Find all rollback intents by user ID.
     */
    List<RollbackIntent> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Delete rollback intent after processing.
     */
    void deleteByProviderIdAndRollbackTransactionId(Long providerId, String rollbackTransactionId);
}
