package com.merchant.transaction.infrastructure.web;

import com.merchant.transaction.application.command.CreateTransactionCommand;
import com.merchant.transaction.application.command.CreateTransactionHandler;
import com.merchant.transaction.application.command.dto.TransactionRequest;
import com.merchant.transaction.application.command.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionCommandController {

    private final CreateTransactionHandler createTransactionHandler;

    @PostMapping
    public Mono<ResponseEntity<TransactionResponse>> createTransaction(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody TransactionRequest request
    ) {
        log.info("Received transaction creation request with idempotency key: {}", idempotencyKey);

        CreateTransactionCommand command = new CreateTransactionCommand(request, idempotencyKey);

        return createTransactionHandler.handle(command)
                .map(response ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response))
                .doOnSuccess(response ->
                        log.info("Transaction created successfully: {}", response.getBody().getId()));
    }

}
