package com.casino.platform.admin.dto;

import com.casino.platform.user.entity.UserRole;
import com.casino.platform.user.entity.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserAdminResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public UserAdminResponse() {}

    public UserAdminResponse(Long id, String username, String email, UserRole role,
                             UserStatus status, BigDecimal balance, LocalDateTime createdAt,
                             LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
