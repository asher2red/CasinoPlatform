package com.casino.platform.provider.service;

import com.casino.platform.provider.entity.PlayerProviderMapping;
import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.exception.ProviderNotFoundException;
import com.casino.platform.provider.repository.PlayerProviderMappingRepository;
import com.casino.platform.provider.repository.ProviderRepository;
import com.casino.platform.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Provider configuration and player mapping service.
 * Handles provider lookup and player ID generation.
 */
@Service
public class ProviderConfigService {

    private final ProviderRepository providerRepository;
    private final PlayerProviderMappingRepository mappingRepository;

    public ProviderConfigService(
            ProviderRepository providerRepository,
            PlayerProviderMappingRepository mappingRepository) {
        this.providerRepository = providerRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Get provider by code.
     * Throws ProviderNotFoundException if not found or inactive.
     */
    @Transactional(readOnly = true)
    public Provider getProviderByCode(String code) {
        return providerRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new ProviderNotFoundException("Provider not found or inactive: " + code));
    }

    /**
     * Get provider by key ID for authentication.
     */
    @Transactional(readOnly = true)
    public Provider getProviderByKeyId(String keyId) {
        return providerRepository.findByKeyId(keyId)
                .orElseThrow(() -> new ProviderNotFoundException("Provider not found for key ID: " + keyId));
    }

    /**
     * Get or create player provider mapping.
     * Returns existing mapping if present, creates new one if not.
     */
    @Transactional
    public PlayerProviderMapping getOrCreatePlayerMapping(User user, Provider provider) {
        return mappingRepository.findByUserIdAndProviderId(user.getId(), provider.getId())
                .orElseGet(() -> createPlayerMapping(user, provider));
    }

    /**
     * Create new player provider mapping with generated provider player ID.
     */
    private PlayerProviderMapping createPlayerMapping(User user, Provider provider) {
        String providerPlayerId = generateProviderPlayerId(user, provider);
        PlayerProviderMapping mapping = new PlayerProviderMapping(user, provider, providerPlayerId);
        return mappingRepository.save(mapping);
    }

    /**
     * Generate provider-specific player ID.
     * Format: {providerCode}-{userId}-{randomSuffix}
     */
    private String generateProviderPlayerId(User user, Provider provider) {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%d-%s", provider.getCode(), user.getId(), randomSuffix);
    }

    /**
     * Get existing player mapping by provider player ID.
     */
    @Transactional(readOnly = true)
    public PlayerProviderMapping getPlayerMappingByProviderPlayerId(Long providerId, String providerPlayerId) {
        return mappingRepository.findByProviderIdAndProviderPlayerId(providerId, providerPlayerId)
                .orElseThrow(() -> new ProviderNotFoundException(
                        "Player mapping not found for provider player ID: " + providerPlayerId));
    }

    /**
     * Validate that provider exists and is active.
     */
    @Transactional(readOnly = true)
    public void validateProvider(String providerCode) {
        if (!providerRepository.existsByCode(providerCode)) {
            throw new ProviderNotFoundException("Provider does not exist: " + providerCode);
        }

        Provider provider = getProviderByCode(providerCode);
        if (!provider.getIsActive()) {
            throw new ProviderNotFoundException("Provider is not active: " + providerCode);
        }
    }
}
