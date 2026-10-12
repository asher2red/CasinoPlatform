package com.casino.platform.provider.exception;

import org.springframework.http.HttpStatus;

/**
 * Error codes for provider wallet API operations.
 */
public enum ProviderErrorCode {

    SUCCESS(HttpStatus.OK, "Success"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request format"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "Invalid HMAC signature"),
    INVALID_TIMESTAMP(HttpStatus.UNAUTHORIZED, "Timestamp expired or invalid"),
    NONCE_REUSED(HttpStatus.UNAUTHORIZED, "Nonce already used"),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "Player not found"),
    INVALID_SESSION(HttpStatus.UNAUTHORIZED, "Invalid or expired session"),
    UNSUPPORTED_CURRENCY(HttpStatus.BAD_REQUEST, "Unsupported currency"),
    INSUFFICIENT_FUNDS(HttpStatus.FORBIDDEN, "Insufficient balance"),
    TRANSACTION_CONFLICT(HttpStatus.CONFLICT, "Transaction ID conflict"),
    ORIGINAL_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Original transaction not found"),
    PLAYER_SUSPENDED(HttpStatus.FORBIDDEN, "Player account suspended"),
    GAME_INACTIVE(HttpStatus.BAD_REQUEST, "Game is inactive"),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Invalid amount"),
    RETRYABLE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Temporary error, please retry");

    private final HttpStatus httpStatus;
    private final String message;

    ProviderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return this.name();
    }
}
