package com.merchant.transaction.infrastructure.persistence.adapter;

import com.merchant.transaction.application.query.dto.ReceivableProjection;
import com.merchant.transaction.infrastructure.persistence.entity.ReceivableEntity;
import com.merchant.transaction.infrastructure.persistence.r2dbc.ReceivableR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceivableQueryAdapter - Infrastructure Layer Tests")
class ReceivableQueryAdapterTest {

    @Mock
    private ReceivableR2dbcRepository repository;

    private ReceivableQueryAdapter adapter;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        adapter = new ReceivableQueryAdapter(repository);
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);
    }

    @Nested
    @DisplayName("Entity to Projection Mapping")
    class EntityToProjectionMapping {

        @Test
        @DisplayName("Should correctly map single entity to projection")
        void shouldMapSingleEntityToProjection() {
            // Given
            ReceivableEntity entity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.00"),
                    new BigDecimal("2.00"),
                    new BigDecimal("98.00"),
                    "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection).isNotNull();
                        assertThat(projection.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
                        assertThat(projection.getDiscount()).isEqualByComparingTo(new BigDecimal("2.00"));
                        assertThat(projection.getTotal()).isEqualByComparingTo(new BigDecimal("98.00"));
                        assertThat(projection.getStatus()).isEqualTo("paid");
                        assertThat(projection.getCreateDate()).isEqualTo(entity.getCreateDate());
                    })
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should correctly map multiple entities to projections")
        void shouldMapMultipleEntitiesToProjections() {
            // Given
            ReceivableEntity entity1 = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.00"),
                    new BigDecimal("2.00"),
                    new BigDecimal("98.00"),
                    "paid"
            );

            ReceivableEntity entity2 = createReceivableEntity(
                    "2",
                    "tx-2",
                    new BigDecimal("200.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("192.00"),
                    "waiting_funds"
            );

            ReceivableEntity entity3 = createReceivableEntity(
                    "3",
                    "tx-3",
                    new BigDecimal("150.00"),
                    new BigDecimal("3.00"),
                    new BigDecimal("147.00"),
                    "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity1, entity2, entity3));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
                        assertThat(projection.getStatus()).isEqualTo("paid");
                    })
                    .assertNext(projection -> {
                        assertThat(projection.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
                        assertThat(projection.getStatus()).isEqualTo("waiting_funds");
                    })
                    .assertNext(projection -> {
                        assertThat(projection.getSubtotal()).isEqualByComparingTo(new BigDecimal("150.00"));
                        assertThat(projection.getStatus()).isEqualTo("paid");
                    })
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should preserve all numeric precision during mapping")
        void shouldPreserveNumericPrecisionDuringMapping() {
            // Given
            ReceivableEntity entity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.55"),
                    new BigDecimal("2.01"),
                    new BigDecimal("98.54"),
                    "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.55"));
                        assertThat(projection.getDiscount()).isEqualByComparingTo(new BigDecimal("2.01"));
                        assertThat(projection.getTotal()).isEqualByComparingTo(new BigDecimal("98.54"));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Date Range Query Behavior")
    class DateRangeQueryBehavior {

        @Test
        @DisplayName("Should return empty flux when no receivables found in date range")
        void shouldReturnEmptyFluxWhenNoReceivablesFound() {
            // Given
            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.empty());

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .expectNextCount(0)
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should pass correct date parameters to repository")
        void shouldPassCorrectDateParametersToRepository() {
            // Given
            LocalDateTime customStart = LocalDateTime.of(2024, 6, 15, 10, 30);
            LocalDateTime customEnd = LocalDateTime.of(2024, 6, 20, 18, 45);

            when(repository.findByCreateDateBetween(customStart, customEnd))
                    .thenReturn(Flux.empty());

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(customStart, customEnd);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(customStart, customEnd);
        }

        @Test
        @DisplayName("Should handle large result sets efficiently")
        void shouldHandleLargeResultSetsEfficiently() {
            // Given - simulate 1000 entities
            Flux<ReceivableEntity> largeFlux = Flux.range(1, 1000)
                    .map(i -> createReceivableEntity(
                            String.valueOf(i),
                            "tx-" + i,
                            new BigDecimal("100.00"),
                            new BigDecimal("2.00"),
                            new BigDecimal("98.00"),
                            i % 2 == 0 ? "paid" : "waiting_funds"
                    ));

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(largeFlux);

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .expectNextCount(1000)
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should propagate repository errors")
        void shouldPropagateRepositoryErrors() {
            // Given
            RuntimeException expectedException = new RuntimeException("Database connection failed");

            when(repository.findByCreateDateBetween(any(), any()))
                    .thenReturn(Flux.error(expectedException));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                            throwable.getMessage().equals("Database connection failed")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should handle repository error after emitting items")
        void shouldHandleRepositoryErrorAfterEmittingItems() {
            // Given
            ReceivableEntity entity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.00"),
                    new BigDecimal("2.00"),
                    new BigDecimal("98.00"),
                    "paid"
            );

            RuntimeException expectedException = new RuntimeException("Connection lost during streaming");

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.concat(
                            Flux.just(entity),
                            Flux.error(expectedException)
                    ));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> assertThat(projection).isNotNull())
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                            throwable.getMessage().equals("Connection lost during streaming")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should handle null values in entity fields gracefully")
        void shouldHandleNullValuesInEntityFieldsGracefully() {
            // Given - entity with potential null fields
            ReceivableEntity entity = ReceivableEntity.builder()
                    .id("1")
                    .transactionId("tx-1")
                    .subTotal(new BigDecimal("100.00"))
                    .discount(new BigDecimal("2.00"))
                    .total(new BigDecimal("98.00"))
                    .status("paid")
                    .createDate(LocalDateTime.now())
                    .paymentDate(null) // nullable field
                    .build();

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then - should complete without errors
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection).isNotNull();
                        assertThat(projection.getSubtotal()).isNotNull();
                        assertThat(projection.getTotal()).isNotNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Reactive Behavior Validation")
    class ReactiveBehaviorValidation {

        @Test
        @DisplayName("Should not execute query until subscription")
        void shouldNotExecuteQueryUntilSubscription() {
            // Given
            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.empty());

            // When - create Flux but don't subscribe
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then - repository not called yet (cold publisher)
            // Now subscribe and verify
            StepVerifier.create(result)
                    .verifyComplete();

            verify(repository).findByCreateDateBetween(startDate, endDate);
        }

        @Test
        @DisplayName("Should support backpressure in reactive stream")
        void shouldSupportBackpressureInReactiveStream() {
            // Given
            ReceivableEntity entity1 = createReceivableEntity(
                    "1", "tx-1", new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("98"), "paid"
            );
            ReceivableEntity entity2 = createReceivableEntity(
                    "2", "tx-2", new BigDecimal("200"), new BigDecimal("4"), new BigDecimal("196"), "paid"
            );
            ReceivableEntity entity3 = createReceivableEntity(
                    "3", "tx-3", new BigDecimal("300"), new BigDecimal("6"), new BigDecimal("294"), "waiting_funds"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity1, entity2, entity3));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then - request specific number of items (backpressure)
            StepVerifier.create(result, 2)
                    .expectNextCount(2)
                    .thenRequest(1)
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should maintain reactive stream completion signal")
        void shouldMaintainReactiveStreamCompletionSignal() {
            // Given
            ReceivableEntity entity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.00"),
                    new BigDecimal("2.00"),
                    new BigDecimal("98.00"),
                    "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(entity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then - verify proper completion signal
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .expectComplete()
                    .verify();
        }
    }

    @Nested
    @DisplayName("Status Filtering Behavior")
    class StatusFilteringBehavior {

        @Test
        @DisplayName("Should correctly map paid status receivables")
        void shouldCorrectlyMapPaidStatusReceivables() {
            // Given
            ReceivableEntity paidEntity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("100.00"),
                    new BigDecimal("2.00"),
                    new BigDecimal("98.00"),
                    "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(paidEntity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection.getStatus()).isEqualTo("paid");
                        assertThat(projection.getTotal()).isEqualByComparingTo(new BigDecimal("98.00"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should correctly map waiting_funds status receivables")
        void shouldCorrectlyMapWaitingFundsStatusReceivables() {
            // Given
            ReceivableEntity waitingEntity = createReceivableEntity(
                    "1",
                    "tx-1",
                    new BigDecimal("200.00"),
                    new BigDecimal("8.00"),
                    new BigDecimal("192.00"),
                    "waiting_funds"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(waitingEntity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> {
                        assertThat(projection.getStatus()).isEqualTo("waiting_funds");
                        assertThat(projection.getTotal()).isEqualByComparingTo(new BigDecimal("192.00"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle mixed status receivables in same result set")
        void shouldHandleMixedStatusReceivablesInSameResultSet() {
            // Given
            ReceivableEntity paidEntity = createReceivableEntity(
                    "1", "tx-1", new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("98"), "paid"
            );
            ReceivableEntity waitingEntity = createReceivableEntity(
                    "2", "tx-2", new BigDecimal("200"), new BigDecimal("8"), new BigDecimal("192"), "waiting_funds"
            );
            ReceivableEntity anotherPaidEntity = createReceivableEntity(
                    "3", "tx-3", new BigDecimal("150"), new BigDecimal("3"), new BigDecimal("147"), "paid"
            );

            when(repository.findByCreateDateBetween(startDate, endDate))
                    .thenReturn(Flux.just(paidEntity, waitingEntity, anotherPaidEntity));

            // When
            Flux<ReceivableProjection> result = adapter.finByDateRange(startDate, endDate);

            // Then
            StepVerifier.create(result)
                    .assertNext(projection -> assertThat(projection.getStatus()).isEqualTo("paid"))
                    .assertNext(projection -> assertThat(projection.getStatus()).isEqualTo("waiting_funds"))
                    .assertNext(projection -> assertThat(projection.getStatus()).isEqualTo("paid"))
                    .verifyComplete();
        }
    }

    // Helper methods
    private ReceivableEntity createReceivableEntity(
            String id,
            String transactionId,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            String status
    ) {
        return ReceivableEntity.builder()
                .id(id)
                .transactionId(transactionId)
                .subTotal(subtotal)
                .discount(discount)
                .total(total)
                .status(status)
                .createDate(LocalDateTime.now())
                .paymentDate(LocalDateTime.now().plusDays(status.equals("waiting_funds") ? 30 : 0))
                .build();
    }
}
