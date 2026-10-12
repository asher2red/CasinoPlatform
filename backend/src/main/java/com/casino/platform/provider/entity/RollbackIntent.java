package com.casino.platform.provider.entity;

import com.casino.platform.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records rollback requests that arrived before the original transaction.
 * Handles out-of-order rollback scenario.
 */
@Entity
@Table(name = "rollback_intents")
public class RollbackIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(name = "rollback_transaction_id", nullable = false, length = 200)
    private String rollbackTransactionId;

    @Column(name = "original_transaction_id", nullable = false, length = 200)
    private String originalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public RollbackIntent() {
    }

    public RollbackIntent(
            Provider provider,
            String rollbackTransactionId,
            String originalTransactionId,
            User user) {
        this.provider = provider;
        this.rollbackTransactionId = rollbackTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getRollbackTransactionId() {
        return rollbackTransactionId;
    }

    public void setRollbackTransactionId(String rollbackTransactionId) {
        this.rollbackTransactionId = rollbackTransactionId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
