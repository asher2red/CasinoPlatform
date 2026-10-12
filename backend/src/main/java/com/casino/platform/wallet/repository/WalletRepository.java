package com.casino.platform.wallet.repository;

import com.casino.platform.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(Long id);

    @Query("SELECT w FROM Wallet w WHERE w.user.username = :username")
    Optional<Wallet> findByUsername(String username);
}
