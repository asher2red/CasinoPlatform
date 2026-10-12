package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.Nonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NonceRepository extends JpaRepository<Nonce, Long> {

    Optional<Nonce> findByProviderIdAndNonce(Long providerId, String nonce);

    boolean existsByProviderIdAndNonce(Long providerId, String nonce);

    @Modifying
    @Query("DELETE FROM Nonce n WHERE n.expiresAt < :now")
    int deleteExpiredNonces(@Param("now") LocalDateTime now);
}
