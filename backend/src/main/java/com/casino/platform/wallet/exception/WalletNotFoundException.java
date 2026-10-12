package com.casino.platform.wallet.exception;

/**
 * Exception thrown when a wallet is not found.
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message) {
        super(message);
    }

    public WalletNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
