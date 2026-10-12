package com.casino.platform.provider.rollback;

import com.casino.platform.provider.entity.Provider;
import com.casino.platform.provider.entity.RollbackIntent;
import com.casino.platform.provider.repository.RollbackIntentRepository;
import com.casino.platform.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for handling rollback intents.
 * Manages out-of-order rollback scenario (rollback arrives before original transaction).
 */
@Service
public class RollbackIntentService {

    private static final Logger log = LoggerFactory.getLogger(RollbackIntentService.class);

    private final RollbackIntentRepository intentRepository;

    public RollbackIntentService(RollbackIntentRepository intentRepository) {
        this.intentRepository = intentRepository;
    }

    /**
     * Save rollback intent when original transaction not found.
     * Returns true if intent saved successfully, false if already exists.
     */
    @Transactional
    public boolean saveRollbackIntent(
            Provider provider,
            String rollbackTransactionId,
            String originalTransactionId,
            User user) {

        // Check if intent already exists
        if (intentRepository.existsByProviderIdAndRollbackTransactionId(
                provider.getId(), rollbackTransactionId)) {
            log.info("Rollback intent already exists: {}", rollbackTransactionId);
            return false;
        }

        try {
            RollbackIntent intent = new RollbackIntent(
                    provider,
                    rollbackTransactionId,
                    originalTransactionId,
                    user
            );
            intentRepository.save(intent);
            log.info("Rollback intent saved: rollbackTxId={}, originalTxId={}",
                    rollbackTransactionId, originalTransactionId);
            return true;

        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread saved the same intent
            log.info("Rollback intent already saved by concurrent request: {}", rollbackTransactionId);
            return false;
        }
    }

    /**
     * Check if rollback intent exists for a transaction.
     */
    @Transactional(readOnly = true)
    public boolean hasRollbackIntent(Provider provider, String originalTransactionId) {
        List<RollbackIntent> intents = intentRepository
                .findByProviderIdAndOriginalTransactionId(provider.getId(), originalTransactionId);
        return !intents.isEmpty();
    }

    /**
     * Get all rollback intents for a specific original transaction.
     */
    @Transactional(readOnly = true)
    public List<RollbackIntent> getRollbackIntents(Provider provider, String originalTransactionId) {
        return intentRepository.findByProviderIdAndOriginalTransactionId(
                provider.getId(), originalTransactionId);
    }

    /**
     * Delete rollback intent after processing.
     */
    @Transactional
    public void deleteRollbackIntent(Provider provider, String rollbackTransactionId) {
        try {
            intentRepository.deleteByProviderIdAndRollbackTransactionId(
                    provider.getId(), rollbackTransactionId);
            log.info("Rollback intent deleted: {}", rollbackTransactionId);
        } catch (Exception e) {
            log.warn("Failed to delete rollback intent: {}", rollbackTransactionId, e);
        }
    }

    /**
     * Find specific rollback intent.
     */
    @Transactional(readOnly = true)
    public Optional<RollbackIntent> findRollbackIntent(Provider provider, String rollbackTransactionId) {
        return intentRepository.findByProviderIdAndRollbackTransactionId(
                provider.getId(), rollbackTransactionId);
    }
}
