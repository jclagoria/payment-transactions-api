package com.merchant.transaction.domain.exception;

public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }

}
