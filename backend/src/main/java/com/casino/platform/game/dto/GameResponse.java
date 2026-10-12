package com.casino.platform.game.dto;

import com.casino.platform.game.entity.Game;
import com.casino.platform.game.entity.GameCategory;
import com.casino.platform.game.entity.GameStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GameResponse {

    private Long id;
    private String name;
    private GameCategory category;
    private String description;
    private String thumbnailUrl;
    private GameStatus status;
    private BigDecimal minBet;
    private BigDecimal maxBet;
    private LocalDateTime createdAt;

    // Constructors
    public GameResponse() {
    }

    public GameResponse(Long id, String name, GameCategory category, String description,
                        String thumbnailUrl, GameStatus status, BigDecimal minBet,
                        BigDecimal maxBet, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.createdAt = createdAt;
    }

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getName(),
                game.getCategory(),
                game.getDescription(),
                game.getThumbnailUrl(),
                game.getStatus(),
                game.getMinBet(),
                game.getMaxBet(),
                game.getCreatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameCategory getCategory() {
        return category;
    }

    public void setCategory(GameCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public BigDecimal getMinBet() {
        return minBet;
    }

    public void setMinBet(BigDecimal minBet) {
        this.minBet = minBet;
    }

    public BigDecimal getMaxBet() {
        return maxBet;
    }

    public void setMaxBet(BigDecimal maxBet) {
        this.maxBet = maxBet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
