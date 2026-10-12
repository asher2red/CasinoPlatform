package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.PlayerProviderMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerProviderMappingRepository extends JpaRepository<PlayerProviderMapping, Long> {

    Optional<PlayerProviderMapping> findByUserIdAndProviderId(Long userId, Long providerId);

    Optional<PlayerProviderMapping> findByProviderIdAndProviderPlayerId(Long providerId, String providerPlayerId);

    List<PlayerProviderMapping> findByUserId(Long userId);

    List<PlayerProviderMapping> findByProviderId(Long providerId);

    boolean existsByUserIdAndProviderId(Long userId, Long providerId);
}
