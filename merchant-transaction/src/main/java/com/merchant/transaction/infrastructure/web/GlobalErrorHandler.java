package com.merchant.transaction.infrastructure.web;

import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.domain.exception.InvalidTransactionException;
import com.merchant.transaction.domain.exception.TransactionCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(InvalidTransactionException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleInvalidTransaction(
            InvalidTransactionException ex) {
        log.error("Invalid transaction: {}", ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST)));
    }

    @ExceptionHandler(TransactionCreationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleTransactionCreation(
            TransactionCreationException ex) {
        log.error("Transaction creation failed: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @ExceptionHandler(IdGenerationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIdGeneration(
            IdGenerationException ex) {
        log.error("ID generation failed: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(
                        "Service temporarily unavailable. Please retry.",
                        HttpStatus.SERVICE_UNAVAILABLE)));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidation(
            WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        "An unexpected error occurred",
                        HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    private Map<String, Object> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return response;
    }

}
