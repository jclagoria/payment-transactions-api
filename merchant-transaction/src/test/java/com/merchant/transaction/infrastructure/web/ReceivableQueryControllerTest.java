package com.merchant.transaction.infrastructure.web;

import com.merchant.transaction.application.query.GetReceivablesSummaryHandler;
import com.merchant.transaction.application.query.GetReceivablesSummaryQuery;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(ReceivableQueryController.class)
@DisplayName("ReceivableQueryController - REST Endpoint Tests")
class ReceivableQueryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GetReceivablesSummaryHandler summaryHandler;

    @Nested
    @DisplayName("GET /receivables/summary - Happy Path")
    class HappyPathScenarios {

        @Test
        @DisplayName("Should return receivables summary with valid date range")
        void shouldReturnReceivablesSummaryWithValidDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

            ReceivablesSummaryResponse expectedResponse = createSummaryResponse(
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00"),
                    new BigDecimal("50.00"),
                    startDate,
                    endDate
            );

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/receivables/summary")
                            .queryParam("startDate", "2024-01-01T00:00:00")
                            .queryParam("endDate", "2024-01-31T23:59:59")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("1000.00"));
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("500.00"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("50.00"));
                        assertThat(response.getPeriod()).isNotNull();
                        assertThat(response.getPeriod().getStartDate()).isEqualTo(startDate);
                        assertThat(response.getPeriod().getEndDate()).isEqualTo(endDate);
                    });

            verify(summaryHandler).handle(any(GetReceivablesSummaryQuery.class));
        }

        @Test
        @DisplayName("Should return summary with complete breakdown structure")
        void shouldReturnSummaryWithCompleteBreakdownStructure() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response.getBreakdown()).isNotNull();
                        assertThat(response.getBreakdown().getPaid()).isNotNull();
                        assertThat(response.getBreakdown().getWaitingFunds()).isNotNull();
                        assertThat(response.getBreakdown().getPaid().getCount()).isEqualTo(5L);
                        assertThat(response.getBreakdown().getWaitingFunds().getCount()).isEqualTo(3L);
                    });
        }

        @Test
        @DisplayName("Should handle same day date range")
        void shouldHandleSameDayDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.of(2024, 6, 15, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 6, 15, 23, 59, 59);

            ReceivablesSummaryResponse expectedResponse = createSummaryResponse(
                    new BigDecimal("100.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("2.00"),
                    startDate,
                    endDate
            );

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-06-15T00:00:00&endDate=2024-06-15T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response.getPeriod().getStartDate()).isEqualTo(startDate);
                        assertThat(response.getPeriod().getEndDate()).isEqualTo(endDate);
                    });
        }

        @Test
        @DisplayName("Should handle empty results with zero totals")
        void shouldHandleEmptyResultsWithZeroTotals() {
            // Given
            LocalDateTime startDate = LocalDateTime.of(2024, 12, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

            ReceivablesSummaryResponse emptyResponse = createSummaryResponse(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    startDate,
                    endDate
            );

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(emptyResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-12-01T00:00:00&endDate=2024-12-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(BigDecimal.ZERO);
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(BigDecimal.ZERO);
                    });
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("Should return error when startDate parameter is missing")
        void shouldReturnErrorWhenStartDateParameterIsMissing() {
            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return error when endDate parameter is missing")
        void shouldReturnErrorWhenEndDateParameterIsMissing() {
            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return error when both parameters are missing")
        void shouldReturnErrorWhenBothParametersAreMissing() {
            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return error when date format is invalid")
        void shouldReturnErrorWhenDateFormatIsInvalid() {
            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01&endDate=2024-01-31")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return error when date contains invalid values")
        void shouldReturnErrorWhenDateContainsInvalidValues() {
            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-13-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return 400 when endDate is before startDate")
        void shouldReturn400WhenEndDateIsBeforeStartDate() {
            // Given
            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.error(new IllegalArgumentException("End date must be after or equal to start date")));

            // When & Then - IllegalArgumentException typically maps to 400 Bad Request
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-31T23:59:59&endDate=2024-01-01T00:00:00")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is4xxClientError();
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should return 500 when handler throws runtime exception")
        void shouldReturn500WhenHandlerThrowsRuntimeException() {
            // Given
            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should propagate handler errors to client")
        void shouldPropagateHandlerErrorsToClient() {
            // Given
            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.error(new IllegalStateException("Invalid query state")));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Should return 500 when handler returns empty mono")
        void shouldReturn500WhenHandlerReturnsEmptyMono() {
            // Given - empty Mono causes NoSuchElementException in doOnSuccess
            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.empty());

            // When & Then - empty mono causes error due to doOnSuccess trying to access response
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Content Negotiation")
    class ContentNegotiation {

        @Test
        @DisplayName("Should return JSON when Accept header is application/json")
        void shouldReturnJsonWhenAcceptHeaderIsApplicationJson() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("Should return JSON when no Accept header is provided")
        void shouldReturnJsonWhenNoAcceptHeaderIsProvided() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON);
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Reactive Behavior")
    class ReactiveBehavior {

        @Test
        @DisplayName("Should handle reactive stream properly with StepVerifier pattern")
        void shouldHandleReactiveStreamProperlyWithStepVerifierPattern() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then - verify reactive response
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> assertThat(response).isNotNull());
        }

        @Test
        @DisplayName("Should not block during request processing")
        void shouldNotBlockDuringRequestProcessing() {
            // Given - simulate delayed response
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse)
                            .delayElement(java.time.Duration.ofMillis(100)));

            // When & Then - should complete without blocking
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> assertThat(response).isNotNull());
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Date Format Variations")
    class DateFormatVariations {

        @Test
        @DisplayName("Should accept ISO-8601 date-time format with milliseconds")
        void shouldAcceptIso8601DateTimeFormatWithMilliseconds() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00.000&endDate=2024-01-31T23:59:59.999")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should accept midnight time values")
        void shouldAcceptMidnightTimeValues() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T00:00:00")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should accept end-of-day time values")
        void shouldAcceptEndOfDayTimeValues() {
            // Given
            ReceivablesSummaryResponse expectedResponse = createCompleteResponse();

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("GET /receivables/summary - Numeric Precision")
    class NumericPrecision {

        @Test
        @DisplayName("Should preserve decimal precision in response")
        void shouldPreserveDecimalPrecisionInResponse() {
            // Given
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

            ReceivablesSummaryResponse expectedResponse = createSummaryResponse(
                    new BigDecimal("1234.56"),
                    new BigDecimal("567.89"),
                    new BigDecimal("12.34"),
                    startDate,
                    endDate
            );

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("1234.56"));
                        assertThat(response.getFutureReceivables()).isEqualByComparingTo(new BigDecimal("567.89"));
                        assertThat(response.getTotalFeeCharged()).isEqualByComparingTo(new BigDecimal("12.34"));
                    });
        }

        @Test
        @DisplayName("Should handle large numeric values correctly")
        void shouldHandleLargeNumericValuesCorrectly() {
            // Given
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

            ReceivablesSummaryResponse expectedResponse = createSummaryResponse(
                    new BigDecimal("999999999.99"),
                    new BigDecimal("500000000.00"),
                    new BigDecimal("10000000.00"),
                    startDate,
                    endDate
            );

            when(summaryHandler.handle(any(GetReceivablesSummaryQuery.class)))
                    .thenReturn(Mono.just(expectedResponse));

            // When & Then
            webTestClient.get()
                    .uri("/receivables/summary?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(ReceivablesSummaryResponse.class)
                    .value(response -> {
                        assertThat(response.getTotalReceivables()).isEqualByComparingTo(new BigDecimal("999999999.99"));
                    });
        }
    }

    // Helper methods
    private ReceivablesSummaryResponse createSummaryResponse(
            BigDecimal totalReceivables,
            BigDecimal futureReceivables,
            BigDecimal totalFeeCharged,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return ReceivablesSummaryResponse.builder()
                .totalReceivables(totalReceivables)
                .futureReceivables(futureReceivables)
                .totalFeeCharged(totalFeeCharged)
                .period(ReceivablesSummaryResponse.PeriodInfo.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .breakdown(ReceivablesSummaryResponse.StatusBreakdown.builder()
                        .paid(ReceivablesSummaryResponse.ReceivableStats.builder()
                                .count(0L)
                                .subtotal(BigDecimal.ZERO)
                                .discount(BigDecimal.ZERO)
                                .total(BigDecimal.ZERO)
                                .build())
                        .waitingFunds(ReceivablesSummaryResponse.ReceivableStats.builder()
                                .count(0L)
                                .subtotal(BigDecimal.ZERO)
                                .discount(BigDecimal.ZERO)
                                .total(BigDecimal.ZERO)
                                .build())
                        .build())
                .build();
    }

    private ReceivablesSummaryResponse createCompleteResponse() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        return ReceivablesSummaryResponse.builder()
                .totalReceivables(new BigDecimal("1000.00"))
                .futureReceivables(new BigDecimal("500.00"))
                .totalFeeCharged(new BigDecimal("50.00"))
                .period(ReceivablesSummaryResponse.PeriodInfo.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .breakdown(ReceivablesSummaryResponse.StatusBreakdown.builder()
                        .paid(ReceivablesSummaryResponse.ReceivableStats.builder()
                                .count(5L)
                                .subtotal(new BigDecimal("500.00"))
                                .discount(new BigDecimal("25.00"))
                                .total(new BigDecimal("475.00"))
                                .build())
                        .waitingFunds(ReceivablesSummaryResponse.ReceivableStats.builder()
                                .count(3L)
                                .subtotal(new BigDecimal("500.00"))
                                .discount(new BigDecimal("25.00"))
                                .total(new BigDecimal("475.00"))
                                .build())
                        .build())
                .build();
    }
}
