package com.casino.platform.ledger.repository;

import com.casino.platform.ledger.entity.Ledger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    @Query("SELECT l FROM Ledger l WHERE l.wallet.id = :walletId ORDER BY l.createdAt DESC")
    Page<Ledger> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<Ledger> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Ledger> findByReferenceId(String referenceId);

    boolean existsByReferenceId(String referenceId);
}
