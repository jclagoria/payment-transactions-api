package com.merchant.transaction.domain.exception;

public class IdGenerationException extends RuntimeException {
    public IdGenerationException(String message) {
        super(message);
    }

    public IdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
