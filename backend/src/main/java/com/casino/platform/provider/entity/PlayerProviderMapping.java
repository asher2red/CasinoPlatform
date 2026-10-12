package com.casino.platform.provider.entity;

import com.casino.platform.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps platform users to provider-specific player IDs.
 * Each user has one player ID per provider.
 */
@Entity
@Table(name = "player_provider_mappings")
public class PlayerProviderMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(name = "provider_player_id", nullable = false, length = 100)
    private String providerPlayerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public PlayerProviderMapping() {
    }

    public PlayerProviderMapping(User user, Provider provider, String providerPlayerId) {
        this.user = user;
        this.provider = provider;
        this.providerPlayerId = providerPlayerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getProviderPlayerId() {
        return providerPlayerId;
    }

    public void setProviderPlayerId(String providerPlayerId) {
        this.providerPlayerId = providerPlayerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
