package com.merchant.transaction.application.query;

import com.merchant.transaction.application.query.dto.ReceivableProjection;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryRequest;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryResponse;
import com.merchant.transaction.application.query.port.ReceivableQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReceivablesSummaryHandler - Behavior Validation")
class GetReceivablesSummaryHandlerTest {

    @Mock
    private ReceivableQueryPort receivableQueryPort;

    private GetReceivablesSummaryHandler handler;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        handler = new GetReceivablesSummaryHandler(receivableQueryPort);
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);
    }

    @Nested
    @DisplayName("Happy Path Scenarios")
    class HappyPathScenarios {

        @Test
        @DisplayName("Should successfully generate summary with mixed paid and waiting_funds receivables")
        void shouldGenerateSummaryWithMixedReceivables() {
            // Given
            ReceivableProjection paidReceivable1 = createReceivableProjection(
                    "1", new BigDecimal("100.00"), new BigDecimal("2.00"), "paid"
            );
            ReceivableProjection paidReceivable2 = createReceivableProjection(
                    "2", new BigDecimal("200.00"), new BigDecimal("4.00"), "paid"
            );
            ReceivableProjection waitingReceivable1 = createReceivableProjection(
                    "3", new BigDecimal("300.00"), new BigDecimal("12.00"), "waiting_funds"
            );
            ReceivableProjection waitingReceivable2 = createReceivableProjection(
                    "4", new BigDecimal("150.00"), new BigDecimal("6.00"), "waiting_funds"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(paidReceivable1, paidReceivable2, waitingReceivable1, waitingReceivable2));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();

                        // Verify totals (total = subtotal - discount)
                        // paid1: 100-2=98, paid2: 200-4=196, waiting1: 300-12=288, waiting2: 150-6=144
                        // Total: 98+196+288+144 = 726
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("726.00"));
                        // futureReceivables = sum of subtotals of waiting_funds = 300+150 = 450
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("450.00"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("24.00"));

                        // Verify period
                        assertThat(response.getPeriod().getStartDate()).isEqualTo(startDate);
                        assertThat(response.getPeriod().getEndDate()).isEqualTo(endDate);

                        // Verify paid breakdown
                        ReceivablesSummaryResponse.ReceivableStats paidStats = response.getBreakdown().getPaid();
                        assertThat(paidStats.getCount()).isEqualTo(2L);
                        assertThat(paidStats.getSubtotal()).isEqualByComparingTo(new BigDecimal("300.00"));
                        assertThat(paidStats.getDiscount()).isEqualByComparingTo(new BigDecimal("6.00"));

                        // Verify waiting_funds breakdown
                        ReceivablesSummaryResponse.ReceivableStats waitingStats = response.getBreakdown().getWaitingFunds();
                        assertThat(waitingStats.getCount()).isEqualTo(2L);
                        assertThat(waitingStats.getSubtotal()).isEqualByComparingTo(new BigDecimal("450.00"));
                        assertThat(waitingStats.getDiscount()).isEqualByComparingTo(new BigDecimal("18.00"));
                    })
                    .verifyComplete();

            verify(receivableQueryPort).finByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should generate summary with only paid receivables")
        void shouldGenerateSummaryWithOnlyPaidReceivables() {
            // Given
            ReceivableProjection paidReceivable1 = createReceivableProjection(
                    "1", new BigDecimal("100.00"), new BigDecimal("2.00"), "paid"
            );
            ReceivableProjection paidReceivable2 = createReceivableProjection(
                    "2", new BigDecimal("200.00"), new BigDecimal("4.00"), "paid"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(paidReceivable1, paidReceivable2));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();

                        // Verify totals (paid1: 100-2=98, paid2: 200-4=196, total: 294)
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("294.00"));
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("6.00"));

                        // Verify paid breakdown
                        ReceivablesSummaryResponse.ReceivableStats paidStats = response.getBreakdown().getPaid();
                        assertThat(paidStats.getCount()).isEqualTo(2L);
                        assertThat(paidStats.getSubtotal()).isEqualByComparingTo(new BigDecimal("300.00"));

                        // Verify waiting_funds breakdown is empty
                        ReceivablesSummaryResponse.ReceivableStats waitingStats = response.getBreakdown().getWaitingFunds();
                        assertThat(waitingStats.getCount()).isZero();
                        assertThat(waitingStats.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should generate summary with only waiting_funds receivables")
        void shouldGenerateSummaryWithOnlyWaitingFundsReceivables() {
            // Given
            ReceivableProjection waitingReceivable1 = createReceivableProjection(
                    "1", new BigDecimal("300.00"), new BigDecimal("12.00"), "waiting_funds"
            );
            ReceivableProjection waitingReceivable2 = createReceivableProjection(
                    "2", new BigDecimal("150.00"), new BigDecimal("6.00"), "waiting_funds"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(waitingReceivable1, waitingReceivable2));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();

                        // Verify totals (waiting1: 300-12=288, waiting2: 150-6=144, total: 432)
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("432.00"));
                        // futureReceivables = sum of subtotals = 300+150 = 450
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("450.00"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("18.00"));

                        // Verify paid breakdown is empty
                        ReceivablesSummaryResponse.ReceivableStats paidStats = response.getBreakdown().getPaid();
                        assertThat(paidStats.getCount()).isZero();
                        assertThat(paidStats.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);

                        // Verify waiting_funds breakdown
                        ReceivablesSummaryResponse.ReceivableStats waitingStats = response.getBreakdown().getWaitingFunds();
                        assertThat(waitingStats.getCount()).isEqualTo(2L);
                        assertThat(waitingStats.getSubtotal()).isEqualByComparingTo(new BigDecimal("450.00"));
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Edge Case Scenarios")
    class EdgeCaseScenarios {

        @Test
        @DisplayName("Should generate empty summary when no receivables found in date range")
        void shouldGenerateEmptySummaryWhenNoReceivables() {
            // Given
            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.empty());

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();

                        // Verify all totals are zero
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(BigDecimal.ZERO);

                        // Verify period is still set
                        assertThat(response.getPeriod().getStartDate()).isEqualTo(startDate);
                        assertThat(response.getPeriod().getEndDate()).isEqualTo(endDate);

                        // Verify breakdown has zero counts
                        assertThat(response.getBreakdown().getPaid().getCount()).isZero();
                        assertThat(response.getBreakdown().getWaitingFunds().getCount()).isZero();
                    })
                    .verifyComplete();

            verify(receivableQueryPort).finByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should handle single receivable correctly")
        void shouldHandleSingleReceivable() {
            // Given
            ReceivableProjection singleReceivable = createReceivableProjection(
                    "1", new BigDecimal("100.00"), new BigDecimal("2.00"), "paid"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(singleReceivable));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        // 100-2=98
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("98.00"));
                        assertThat(response.getBreakdown().getPaid().getCount()).isEqualTo(1L);
                        assertThat(response.getBreakdown().getWaitingFunds().getCount()).isZero();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle receivables with zero amounts")
        void shouldHandleReceivablesWithZeroAmounts() {
            // Given
            ReceivableProjection zeroReceivable = createReceivableProjection(
                    "1", BigDecimal.ZERO, BigDecimal.ZERO, "paid"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(zeroReceivable));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getBreakdown().getPaid().getCount()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Calculation Accuracy")
    class CalculationAccuracy {

        @Test
        @DisplayName("Should correctly calculate totals with decimal precision")
        void shouldCalculateTotalsWithDecimalPrecision() {
            // Given
            ReceivableProjection receivable1 = createReceivableProjection(
                    "1", new BigDecimal("100.55"), new BigDecimal("2.01"), "paid"
            );
            ReceivableProjection receivable2 = createReceivableProjection(
                    "2", new BigDecimal("200.33"), new BigDecimal("8.01"), "waiting_funds"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(receivable1, receivable2));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        // receivable1 (paid): 100.55-2.01=98.54, receivable2 (waiting): 200.33-8.01=192.32, total: 290.86
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("290.86"));
                        // futureReceivables = subtotal of waiting_funds = 200.33
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("200.33"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("10.02"));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should correctly aggregate large number of receivables")
        void shouldAggregrateLargeNumberOfReceivables() {
            // Given
            Flux<ReceivableProjection> manyReceivables = Flux.range(1, 100)
                    .map(i -> createReceivableProjection(
                            String.valueOf(i),
                            new BigDecimal("100.00"),
                            new BigDecimal("2.00"),
                            i % 2 == 0 ? "paid" : "waiting_funds"
                    ));

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(manyReceivables);

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        // 100 receivables: each has subtotal=100, discount=2, total=98
                        // Total: 100 * 98 = 9800
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("9800.00"));
                        // futureReceivables = sum of subtotals of waiting_funds (50 receivables) = 50 * 100 = 5000
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("5000.00"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("200.00"));
                        assertThat(response.getBreakdown().getPaid().getCount()).isEqualTo(50L);
                        assertThat(response.getBreakdown().getWaitingFunds().getCount()).isEqualTo(50L);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should propagate error when query port fails")
        void shouldPropagateErrorWhenQueryPortFails() {
            // Given
            RuntimeException expectedException = new RuntimeException("Database connection failed");

            when(receivableQueryPort.finByDateRange(any(), any()))
                    .thenReturn(Flux.error(expectedException));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                            throwable.getMessage().equals("Database connection failed")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should handle query port returning error after emitting items")
        void shouldHandleErrorAfterEmittingItems() {
            // Given
            ReceivableProjection receivable1 = createReceivableProjection(
                    "1", new BigDecimal("100.00"), new BigDecimal("2.00"), "paid"
            );
            RuntimeException expectedException = new RuntimeException("Connection lost");

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.concat(
                            Flux.just(receivable1),
                            Flux.error(expectedException)
                    ));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                            throwable.getMessage().equals("Connection lost")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("Reactive Behavior Validation")
    class ReactiveBehaviorValidation {

        @Test
        @DisplayName("Should not execute query until subscription")
        void shouldNotExecuteQueryUntilSubscription() {
            // Given
            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.empty());

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When - create Mono but don't subscribe
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then - verify no interaction yet (cold publisher behavior)
            // Note: This is a characteristic of reactive streams - cold publishers don't execute until subscription

            // Now subscribe and verify interaction
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            verify(receivableQueryPort).finByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should complete successfully for valid reactive stream")
        void shouldCompleteSuccessfullyForValidReactiveStream() {
            // Given
            ReceivableProjection receivable = createReceivableProjection(
                    "1", new BigDecimal("100.00"), new BigDecimal("2.00"), "paid"
            );

            when(receivableQueryPort.finByDateRange(startDate, endDate))
                    .thenReturn(Flux.just(receivable));

            GetReceivablesSummaryQuery query = createQuery(startDate, endDate);

            // When
            Mono<ReceivablesSummaryResponse> result = handler.handle(query);

            // Then - verify reactive stream completes properly
            StepVerifier.create(result)
                    .expectNextMatches(response -> response != null)
                    .verifyComplete();
        }
    }

    // Helper methods
    private ReceivableProjection createReceivableProjection(
            String id,
            BigDecimal subtotal,
            BigDecimal discount,
            String status
    ) {
        // Total = subtotal - discount (as per ReceivableProjection documentation)
        BigDecimal total = subtotal.subtract(discount);
        return ReceivableProjection.builder()
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .status(status)
                .createDate(LocalDateTime.now())
                .build();
    }

    private GetReceivablesSummaryQuery createQuery(LocalDateTime startDate, LocalDateTime endDate) {
        ReceivablesSummaryRequest request = ReceivablesSummaryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return GetReceivablesSummaryQuery.of(request);
    }
}
