package com.casino.platform.provider.exception;

/**
 * Exception thrown when a provider is not found or not active.
 */
public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message) {
        super(message);
    }

    public ProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
