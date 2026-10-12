package com.casino.platform.game.dto;

/**
 * Response DTO for game launch.
 * Contains the launch URL and session token.
 */
public class LaunchGameResponse {

    private String launchUrl;
    private String sessionToken;
    private Long sessionId;

    // Constructors
    public LaunchGameResponse() {
    }

    public LaunchGameResponse(String launchUrl, String sessionToken, Long sessionId) {
        this.launchUrl = launchUrl;
        this.sessionToken = sessionToken;
        this.sessionId = sessionId;
    }

    // Static factory method
    public static LaunchGameResponse of(String launchUrl, String sessionToken, Long sessionId) {
        return new LaunchGameResponse(launchUrl, sessionToken, sessionId);
    }

    // Getters and Setters
    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
