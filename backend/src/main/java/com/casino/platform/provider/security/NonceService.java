package com.casino.platform.provider.security;

import com.casino.platform.provider.entity.Nonce;
import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.exception.ProviderAuthenticationException;
import com.casino.platform.provider.repository.NonceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nonce validation service to prevent replay attacks.
 * Ensures each nonce is used only once within its validity period.
 */
@Service
public class NonceService {

    private static final int NONCE_VALIDITY_MINUTES = 5;
    private static final long TIMESTAMP_SKEW_MS = 300_000; // 5 minutes

    private final NonceRepository nonceRepository;

    public NonceService(NonceRepository nonceRepository) {
        this.nonceRepository = nonceRepository;
    }

    /**
     * Validate and store nonce.
     * Throws exception if nonce is reused or timestamp is too old/future.
     */
    @Transactional
    public void validateAndStoreNonce(Provider provider, String nonce, Long timestampMs) {
        // Validate timestamp (prevent old/future requests)
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestampMs) > TIMESTAMP_SKEW_MS) {
            throw new ProviderAuthenticationException("Request timestamp out of acceptable range");
        }

        // Check if nonce already exists (synchronous check)
        if (nonceRepository.existsByProviderIdAndNonce(provider.getId(), nonce)) {
            throw new ProviderAuthenticationException("Nonce already used (replay attack detected)");
        }

        // Store nonce
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(NONCE_VALIDITY_MINUTES);
        Nonce nonceEntity = new Nonce(provider, nonce, timestampMs, expiresAt);

        try {
            nonceRepository.save(nonceEntity);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread saved the same nonce
            throw new ProviderAuthenticationException("Nonce already used (concurrent request detected)");
        }
    }

    /**
     * Clean up expired nonces (scheduled task can call this).
     */
    @Transactional
    public int cleanupExpiredNonces() {
        return nonceRepository.deleteExpiredNonces(LocalDateTime.now());
    }
}
