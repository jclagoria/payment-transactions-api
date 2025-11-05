package com.merchant.transaction.application.command;

import com.merchant.transaction.application.command.dto.ReceivableResponse;
import com.merchant.transaction.application.command.dto.TransactionRequest;
import com.merchant.transaction.application.command.dto.TransactionResponse;
import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.domain.exception.TransactionCreationException;
import com.merchant.transaction.domain.model.Receivable;
import com.merchant.transaction.domain.model.Transaction;
import com.merchant.transaction.domain.service.ReceivableDomainService;
import com.merchant.transaction.domain.service.TransactionDomainService;
import com.merchant.transaction.infrastructure.external.idempotency.IdempotencyService;
import com.merchant.transaction.infrastructure.persistence.r2dbc.ReceivableR2dbcRepository;
import com.merchant.transaction.infrastructure.persistence.r2dbc.TransactionR2dbcRepository;
import com.merchant.transaction.shared.IdGenerationService;
import com.merchant.transaction.shared.mapper.ReceivableMapper;
import com.merchant.transaction.shared.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTransactionHandler {

    private final IdGenerationService idGenerationService;
    private final TransactionDomainService transactionDomainService;
    private final ReceivableDomainService receivableDomainService;
    private final IdempotencyService idempotencyService;
    private final TransactionR2dbcRepository transactionRepository;
    private final ReceivableR2dbcRepository receivableRepository;
    private final TransactionMapper transactionMapper;
    private final ReceivableMapper receivableMapper;
    private final TransactionalOperator transactionalOperator;

    /**
     * Handles transaction creation with idempotency protection.
     */
    public Mono<TransactionResponse> handle(CreateTransactionCommand command) {
        return idempotencyService.executeIdempotent(
                command.getIdempotencyKey(),
                () -> executeTransaction(command.getRequest())
        );
    }

    /**
     * Executes the complete transaction creation flow.
     */
    private Mono<TransactionResponse> executeTransaction(TransactionRequest request) {
        log.info("Starting transaction creation for merchant: {}", request.getMerchantName());

        // STEP 1: Generate unique IDs for transaction and receivable sequentially (to avoid race conditions)
        return idGenerationService.generateUniqueId()
                .flatMap(transactionId ->
                        idGenerationService.generateUniqueId()
                                .map(receivableId -> reactor.util.function.Tuples.of(transactionId, receivableId))
                )
                .doOnNext(tuple -> log.info("Generated IDs - Transaction: {}, Receivable: {}",
                        tuple.getT1(), tuple.getT2()))
                .onErrorMap(IdGenerationException.class,
                        e -> new TransactionCreationException("Failed to generate unique IDs", e))
                .flatMap(tupple -> {
                    String transactionId = tupple.getT1();
                    String receivableId = tupple.getT2();

                    try {
                        // STEP 2: Create Transaction domain object
                        Transaction transaction = transactionDomainService.createTransaction(
                                transactionId,
                                request.getValue(),
                                request.getDescription(),
                                request.getMethod(),
                                request.getCardNumber(),
                                request.getMerchantName(),
                                request.getMerchantName()
                        );

                        // STEP 3: Calculate Receivable based on Transaction
                        Receivable receivable = receivableDomainService.calculateReceivable(
                                receivableId,
                                transactionId,
                                transaction.getValue(),
                                transaction.getPaymentMethod()
                        );

                        log.info("Created transaction and calculated receivable for transaction ID: {}",
                                transactionId);

                        // STEP 4: Save both Transaction and Receivable atomically
                        return saveAtomically(transaction, receivable);
                    } catch (Exception e) {
                        log.error("Error creating domain models", e);
                        return Mono.error(new TransactionCreationException(
                                "Failed to create transaction: " + e.getMessage(), e));
                    }
                })
                // STEP 5: Convert saved Transaction to response DTO (reactive)
                .flatMap(this::toResponse)
                .doOnSuccess(response ->
                        log.info("Successfully created transaction {} with receivable {}",
                                response.getId(), response.getReceivable().getId()))
                .doOnError(error ->
                        log.error("Transaction creation failed", error));
    }

    /**
     * Saves transaction and receivable atomically using R2DBC transaction.
     */
    private Mono<Transaction> saveAtomically(Transaction transaction, Receivable receivable) {
        return transactionalOperator.transactional(
                transactionRepository.save(transactionMapper.toEntity(transaction))
                        .then(receivableRepository.save(receivableMapper.toEntity(receivable)))
                        .thenReturn(transaction)
                )
                .doOnSuccess(t -> log.info("Atomic save successful for transaction {}", t.getId()))
                .onErrorMap(error -> new TransactionCreationException(
                        "Database save failed - transaction rolled back", error));

    }

    /**
     * Maps domain models to response DTO (fully reactive).
     */
    private Mono<TransactionResponse> toResponse(Transaction transaction) {
        return transactionRepository.findById(transaction.getId())
                .zipWith(receivableRepository.findByTransactionId(transaction.getId()))
                .map(tuple -> {
                    var t = transactionMapper.toDomain(tuple.getT1());
                    var r = receivableMapper.toDomain(tuple.getT2());

                    return TransactionResponse.builder()
                            .id(t.getId())
                            .value(t.getValue().toString())
                            .description(t.getDescription())
                            .method(t.getPaymentMethod())
                            .cardNumber(t.getCardNumber())
                            .merchantName(t.getMerchantName())
                            .customerName(t.getCustomerName())
                            .status(t.getStatus())
                            .createdAt(t.getCreatedAt())
                            .receivable(ReceivableResponse.builder()
                                    .id(r.getId())
                                    .status(r.getStatus())
                                    .createDate(t.getCreatedAt())
                                    .paymentDate(r.getPaymentDate())
                                    .subtotal(r.getSubtotal().toString())
                                    .discount(r.getDiscount().toString())
                                    .total(r.getTotal().toString())
                                    .build())
                            .build();
                });
    }

}
