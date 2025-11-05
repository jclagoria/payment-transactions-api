package com.merchant.transaction.application.command;

import com.merchant.transaction.application.command.dto.TransactionRequest;
import com.merchant.transaction.application.command.dto.TransactionResponse;
import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.domain.exception.InvalidTransactionException;
import com.merchant.transaction.domain.exception.TransactionCreationException;
import com.merchant.transaction.domain.model.*;
import com.merchant.transaction.domain.service.ReceivableDomainService;
import com.merchant.transaction.domain.service.TransactionDomainService;
import com.merchant.transaction.infrastructure.external.idempotency.IdempotencyService;
import com.merchant.transaction.infrastructure.persistence.entity.ReceivableEntity;
import com.merchant.transaction.infrastructure.persistence.entity.TransactionEntity;
import com.merchant.transaction.infrastructure.persistence.r2dbc.ReceivableR2dbcRepository;
import com.merchant.transaction.infrastructure.persistence.r2dbc.TransactionR2dbcRepository;
import com.merchant.transaction.shared.IdGenerationService;
import com.merchant.transaction.shared.mapper.ReceivableMapper;
import com.merchant.transaction.shared.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTransactionHandler Unit Tests")
class CreateTransactionHandlerTest {

    @Mock
    private IdGenerationService idGenerationService;

    @Mock
    private TransactionDomainService transactionDomainService;

    @Mock
    private ReceivableDomainService receivableDomainService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionR2dbcRepository transactionRepository;

    @Mock
    private ReceivableR2dbcRepository receivableRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private ReceivableMapper receivableMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private CreateTransactionHandler handler;

    private TransactionRequest validRequest;
    private CreateTransactionCommand validCommand;
    private Transaction mockTransaction;
    private Receivable mockReceivable;
    private TransactionEntity mockTransactionEntity;
    private ReceivableEntity mockReceivableEntity;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new TransactionRequest(
                "100.00",
                "Test transaction",
                PaymentMethod.DEBIT_CARD,
                "1234567890",
                "Test Merchant",
                "Test Customer"
        );

        validCommand = new CreateTransactionCommand(validRequest, "test-idempotency-key");

        // Setup mock transaction
        mockTransaction = Transaction.builder()
                .id("TXN-001")
                .value(new Money("100.00"))
                .description("Test transaction")
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .cardNumber("7890")
                .merchantName("Test Merchant")
                .customerName("Test Customer")
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup mock receivable
        mockReceivable = Receivable.builder()
                .id("RCV-001")
                .transactionId("TXN-001")
                .status("paid")
                .createDate(LocalDateTime.now())
                .paymentDate(LocalDateTime.now())
                .subtotal(new Money("100.00"))
                .discount(new Money("2.00"))
                .total(new Money("98.00"))
                .build();

        // Setup entities
        mockTransactionEntity = new TransactionEntity();
        mockTransactionEntity.setId("TXN-001");

        mockReceivableEntity = new ReceivableEntity();
        mockReceivableEntity.setId("RCV-001");
        mockReceivableEntity.setTransactionId("TXN-001");
    }

    @Nested
    @DisplayName("Successful Transaction Creation Scenarios")
    class SuccessfulTransactionCreation {

        @Test
        @DisplayName("Should create transaction successfully with debit card")
        void shouldCreateTransactionSuccessfullyWithDebitCard() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(mockTransaction);

            when(receivableDomainService.calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            )).thenReturn(mockReceivable);

            when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
            when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

            when(transactionRepository.save(any(TransactionEntity.class)))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.save(any(ReceivableEntity.class)))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionalOperator.transactional(any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(transactionRepository.findById(anyString()))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.findByTransactionId(anyString()))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(mockTransaction);
            when(receivableMapper.toDomain(any(ReceivableEntity.class))).thenReturn(mockReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getId()).isEqualTo("TXN-001");
                        assertThat(response.getMerchantName()).isEqualTo("Test Merchant");
                        assertThat(response.getReceivable()).isNotNull();
                        assertThat(response.getReceivable().getId()).isEqualTo("RCV-001");
                    })
                    .verifyComplete();

            verify(idGenerationService, times(2)).generateUniqueId();
            verify(transactionDomainService, times(1)).createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            );
            verify(receivableDomainService, times(1)).calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            );
        }

        @Test
        @DisplayName("Should create transaction successfully with credit card")
        void shouldCreateTransactionSuccessfullyWithCreditCard() {
            // Arrange
            validRequest.setMethod(PaymentMethod.CREDIT_CARD);

            Transaction creditTransaction = mockTransaction.withPaymentMethod(PaymentMethod.CREDIT_CARD);
            Receivable creditReceivable = Receivable.builder()
                    .id("RCV-002")
                    .transactionId("TXN-002")
                    .status("waiting_funds")
                    .createDate(LocalDateTime.now())
                    .paymentDate(LocalDateTime.now().plusDays(30))
                    .subtotal(new Money("100.00"))
                    .discount(new Money("4.00"))
                    .total(new Money("96.00"))
                    .build();

            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-002"))
                    .thenReturn(Mono.just("RCV-002"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(creditTransaction);

            setupSuccessfulMocks(creditTransaction, creditReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
                        assertThat(response.getReceivable().getStatus()).isEqualTo("waiting_funds");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should use idempotency service to prevent duplicate transactions")
        void shouldUseIdempotencyServiceToPreventDuplicates() {
            // Arrange
            TransactionResponse cachedResponse = TransactionResponse.builder()
                    .id("TXN-CACHED")
                    .build();

            when(idempotencyService.executeIdempotent(eq("test-idempotency-key"), any()))
                    .thenReturn(Mono.just(cachedResponse));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getId()).isEqualTo("TXN-CACHED");
                    })
                    .verifyComplete();

            verify(idempotencyService, times(1)).executeIdempotent(eq("test-idempotency-key"), any());
            verifyNoInteractions(idGenerationService); // Should not generate IDs for cached response
        }

        @Test
        @DisplayName("Should generate two unique IDs in parallel")
        void shouldGenerateTwoUniqueIdsInParallel() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-123"))
                    .thenReturn(Mono.just("RCV-456"));

            setupSuccessfulMocks(mockTransaction, mockReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            verify(idGenerationService, times(2)).generateUniqueId();
        }
    }

    @Nested
    @DisplayName("ID Generation Failure Scenarios")
    class IdGenerationFailures {

        @Test
        @DisplayName("Should fail when ID generation service throws exception")
        void shouldFailWhenIdGenerationThrowsException() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.error(new IdGenerationException("Failed to generate ID")));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getMessage().contains("Failed to generate unique IDs")
                    )
                    .verify();

            verify(idGenerationService, times(1)).generateUniqueId(); // Zip attempts both
            verifyNoInteractions(transactionDomainService);
            verifyNoInteractions(receivableDomainService);
        }

        @Test
        @DisplayName("Should wrap IdGenerationException in TransactionCreationException")
        void shouldWrapIdGenerationExceptionInTransactionCreationException() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            IdGenerationException originalException = new IdGenerationException("Numerator API timeout");

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.error(originalException));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getCause() instanceof IdGenerationException &&
                                    error.getCause().getMessage().equals("Numerator API timeout")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("Domain Validation Failure Scenarios")
    class DomainValidationFailures {

        @Test
        @DisplayName("Should fail when transaction domain service throws validation exception")
        void shouldFailWhenTransactionDomainServiceThrowsException() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenThrow(new InvalidTransactionException("Transaction value must be positive"));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getMessage().contains("Failed to create transaction") &&
                                    error.getMessage().contains("Transaction value must be positive")
                    )
                    .verify();

            verify(transactionDomainService, times(1)).createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            );
            verifyNoInteractions(receivableDomainService);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should fail when invalid card number is provided")
        void shouldFailWhenInvalidCardNumberProvided() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenThrow(new InvalidTransactionException("Card number must have at least 4 digits"));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getMessage().contains("Card number must have at least 4 digits")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("Database Persistence Failure Scenarios")
    class DatabasePersistenceFailures {

        @Test
        @DisplayName("Should fail when transaction repository save fails")
        void shouldFailWhenTransactionRepositorySaveFails() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(mockTransaction);

            when(receivableDomainService.calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            )).thenReturn(mockReceivable);

            when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
            when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

            RuntimeException dbError = new RuntimeException("Database connection failed");
            when(transactionRepository.save(any(TransactionEntity.class)))
                    .thenReturn(Mono.error(dbError));
            when(receivableRepository.save(any(ReceivableEntity.class)))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionalOperator.transactional(any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getMessage().contains("Database save failed - transaction rolled back")
                    )
                    .verify();

            verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
        }

        @Test
        @DisplayName("Should rollback transaction when receivable repository save fails")
        void shouldRollbackWhenReceivableRepositorySaveFails() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(mockTransaction);

            when(receivableDomainService.calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            )).thenReturn(mockReceivable);

            when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
            when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

            when(transactionRepository.save(any(TransactionEntity.class)))
                    .thenReturn(Mono.just(mockTransactionEntity));

            RuntimeException dbError = new RuntimeException("Constraint violation");
            when(receivableRepository.save(any(ReceivableEntity.class)))
                    .thenReturn(Mono.error(dbError));

            when(transactionalOperator.transactional(any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectErrorMatches(error ->
                            error instanceof TransactionCreationException &&
                                    error.getMessage().contains("Database save failed - transaction rolled back")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("Atomic Transaction Scenarios")
    class AtomicTransactionScenarios {

        @Test
        @DisplayName("Should save transaction and receivable atomically")
        void shouldSaveTransactionAndReceivableAtomically() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(mockTransaction);

            setupSuccessfulMocks(mockTransaction, mockReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            verify(transactionalOperator, times(1)).transactional(any(Mono.class));
            verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
            verify(receivableRepository, times(1)).save(any(ReceivableEntity.class));
        }

        @Test
        @DisplayName("Should use transactional operator for atomic saves")
        void shouldUseTransactionalOperatorForAtomicSaves() {
            // Arrange
            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(mockTransaction);

            when(receivableDomainService.calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            )).thenReturn(mockReceivable);

            when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
            when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

            when(transactionRepository.save(any(TransactionEntity.class)))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.save(any(ReceivableEntity.class)))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionalOperator.transactional(any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(transactionRepository.findById(anyString()))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.findByTransactionId(anyString()))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(mockTransaction);
            when(receivableMapper.toDomain(any(ReceivableEntity.class))).thenReturn(mockReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            verify(transactionalOperator, times(1)).transactional(any(Mono.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle minimum transaction value")
        void shouldHandleMinimumTransactionValue() {
            // Arrange
            validRequest.setValue("0.01");

            Transaction minTransaction = mockTransaction
                    .withValue(new Money("0.01"));

            Receivable minReceivable = Receivable.builder()
                    .id("RCV-001")
                    .transactionId("TXN-001")
                    .status("paid")
                    .createDate(LocalDateTime.now())
                    .paymentDate(LocalDateTime.now())
                    .subtotal(new Money("0.01"))
                    .discount(new Money("0.00"))
                    .total(new Money("0.01"))
                    .build();

            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(minTransaction);

            setupSuccessfulMocks(minTransaction, minReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getValue()).isEqualTo("0.01");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle large transaction value")
        void shouldHandleLargeTransactionValue() {
            // Arrange
            validRequest.setValue("999999.99");

            Transaction largeTransaction = mockTransaction
                    .withValue(new Money("999999.99"));

            Receivable largeReceivable = Receivable.builder()
                    .id("RCV-001")
                    .transactionId("TXN-001")
                    .status("paid")
                    .createDate(LocalDateTime.now())
                    .paymentDate(LocalDateTime.now())
                    .subtotal(new Money("999999.99"))
                    .discount(new Money("20000.00"))
                    .total(new Money("979999.99"))
                    .build();

            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
            )).thenReturn(largeTransaction);


            setupSuccessfulMocks(largeTransaction, largeReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getValue()).isEqualTo("999999.99");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle transaction with null description")
        void shouldHandleTransactionWithNullDescription() {
            // Arrange
            validRequest.setDescription(null);

            Transaction noDescTransaction = mockTransaction
                    .withDescription(null);

            when(idempotencyService.executeIdempotent(anyString(), any()))
                    .thenAnswer(invocation -> {
                        Supplier<Mono<TransactionResponse>> supplier = invocation.getArgument(1);
                        return supplier.get();
                    });

            when(idGenerationService.generateUniqueId())
                    .thenReturn(Mono.just("TXN-001"))
                    .thenReturn(Mono.just("RCV-001"));

            when(transactionDomainService.createTransaction(
                    anyString(), anyString(), isNull(), any(), anyString(), anyString(), anyString()
            )).thenReturn(noDescTransaction);

            when(receivableDomainService.calculateReceivable(
                    anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
            )).thenReturn(mockReceivable);

            when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
            when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

            when(transactionRepository.save(any(TransactionEntity.class)))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.save(any(ReceivableEntity.class)))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionalOperator.transactional(any(Mono.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(transactionRepository.findById(anyString()))
                    .thenReturn(Mono.just(mockTransactionEntity));
            when(receivableRepository.findByTransactionId(anyString()))
                    .thenReturn(Mono.just(mockReceivableEntity));

            when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(noDescTransaction);
            when(receivableMapper.toDomain(any(ReceivableEntity.class))).thenReturn(mockReceivable);

            // Act
            Mono<TransactionResponse> result = handler.handle(validCommand);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getDescription()).isNull();
                    })
                    .verifyComplete();
        }
    }

    // Helper method to setup successful mocks
    private void setupSuccessfulMocks(Transaction transaction, Receivable receivable) {
        when(transactionDomainService.createTransaction(
                anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyString()
        )).thenReturn(transaction);

        when(receivableDomainService.calculateReceivable(
                anyString(), anyString(), any(Money.class), any(PaymentMethod.class)
        )).thenReturn(receivable);

        when(transactionMapper.toEntity(any(Transaction.class))).thenReturn(mockTransactionEntity);
        when(receivableMapper.toEntity(any(Receivable.class))).thenReturn(mockReceivableEntity);

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenReturn(Mono.just(mockTransactionEntity));
        when(receivableRepository.save(any(ReceivableEntity.class)))
                .thenReturn(Mono.just(mockReceivableEntity));

        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.findById(anyString()))
                .thenReturn(Mono.just(mockTransactionEntity));
        when(receivableRepository.findByTransactionId(anyString()))
                .thenReturn(Mono.just(mockReceivableEntity));

        when(transactionMapper.toDomain(any(TransactionEntity.class))).thenReturn(transaction);
        when(receivableMapper.toDomain(any(ReceivableEntity.class))).thenReturn(receivable);
    }
}
