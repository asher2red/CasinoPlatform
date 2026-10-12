package com.casino.platform.provider.exception;

/**
 * Exception thrown when provider authentication fails.
 */
public class ProviderAuthenticationException extends RuntimeException {

    public ProviderAuthenticationException(String message) {
        super(message);
    }

    public ProviderAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
