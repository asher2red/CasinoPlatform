package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.ProviderTransaction;
import com.casino.platform.provider.entity.ProviderTransaction.OperationType;
import com.casino.platform.provider.entity.ProviderTransaction.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, Long> {

    /**
     * Find transaction by provider and transaction ID (for idempotency).
     */
    Optional<ProviderTransaction> findByProviderIdAndTransactionId(Long providerId, String transactionId);

    /**
     * Check if transaction exists (for idempotency check).
     */
    boolean existsByProviderIdAndTransactionId(Long providerId, String transactionId);

    /**
     * Find transactions by user ID.
     */
    List<ProviderTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find transactions by round ID.
     */
    List<ProviderTransaction> findByRoundIdOrderByCreatedAtAsc(String roundId);

    /**
     * Find transaction by original transaction ID (for rollback lookup).
     */
    @Query("SELECT pt FROM ProviderTransaction pt WHERE pt.provider.id = :providerId " +
           "AND pt.transactionId = :originalTransactionId " +
           "AND pt.operationType IN ('BET', 'WIN')")
    Optional<ProviderTransaction> findOriginalTransaction(
            @Param("providerId") Long providerId,
            @Param("originalTransactionId") String originalTransactionId);

    /**
     * Find all successful transactions for a round.
     */
    @Query("SELECT pt FROM ProviderTransaction pt WHERE pt.roundId = :roundId " +
           "AND pt.status = 'SUCCESS' ORDER BY pt.createdAt ASC")
    List<ProviderTransaction> findSuccessfulTransactionsByRound(@Param("roundId") String roundId);

    /**
     * Find pending transactions (for cleanup/monitoring).
     */
    List<ProviderTransaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    /**
     * Count transactions by operation type and status.
     */
    long countByOperationTypeAndStatus(OperationType operationType, TransactionStatus status);
}
