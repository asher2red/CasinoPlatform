package com.casino.platform.provider.adapter;

/**
 * Result of game launch operation.
 * Contains the launch URL and any additional metadata.
 */
public class LaunchResult {

    private String launchUrl;
    private String providerGameId;
    private String metadata;

    // Constructors
    public LaunchResult() {
    }

    public LaunchResult(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public LaunchResult(String launchUrl, String providerGameId) {
        this.launchUrl = launchUrl;
        this.providerGameId = providerGameId;
    }

    // Static factory methods
    public static LaunchResult of(String launchUrl) {
        return new LaunchResult(launchUrl);
    }

    public static LaunchResult of(String launchUrl, String providerGameId) {
        return new LaunchResult(launchUrl, providerGameId);
    }

    // Getters and Setters
    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getProviderGameId() {
        return providerGameId;
    }

    public void setProviderGameId(String providerGameId) {
        this.providerGameId = providerGameId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
