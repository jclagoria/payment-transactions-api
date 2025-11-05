package com.merchant.transaction.domain.exception;

public class TransactionCreationException extends RuntimeException {

    public TransactionCreationException(String message) {
        super(message);
    }

    public TransactionCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}
