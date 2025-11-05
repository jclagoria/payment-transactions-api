package com.merchant.transaction.infrastructure.web;

import com.merchant.transaction.application.command.CreateTransactionCommand;
import com.merchant.transaction.application.command.CreateTransactionHandler;
import com.merchant.transaction.application.command.dto.ReceivableResponse;
import com.merchant.transaction.application.command.dto.TransactionRequest;
import com.merchant.transaction.application.command.dto.TransactionResponse;
import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.domain.exception.InvalidTransactionException;
import com.merchant.transaction.domain.exception.TransactionCreationException;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebFluxTest(TransactionCommandController.class)
@DisplayName("TransactionCommandController - Unit Tests")
class TransactionCommandControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateTransactionHandler createTransactionHandler;

    @Test
    @DisplayName("Should create transaction successfully with valid request and idempotency key")
    void shouldCreateTransactionSuccessfully() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        TransactionResponse expectedResponse = buildSuccessfulDebitResponse();

        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TransactionResponse.class)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getId()).isEqualTo("TXN-001");
                    assertThat(response.getValue()).isEqualTo("100.00");
                    assertThat(response.getMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
                    assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
                    assertThat(response.getReceivable()).isNotNull();
                });

        verify(createTransactionHandler, times(1)).handle(argThat(cmd ->
                cmd.getIdempotencyKey().equals("test-key-12345") &&
                        cmd.getRequest().getValue().equals("100.00")
        ));
    }

    @Test
    @DisplayName("Should create transaction successfully with credit card payment method")
    void shouldCreateTransactionWithCreditCard() {
        // Given
        TransactionRequest request = buildValidCreditCardRequest();
        TransactionResponse expectedResponse = buildSuccessfulCreditResponse();

        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "credit-test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .value(response -> {
                    assertThat(response.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
                    assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
                });
    }

    @Test
    @DisplayName("Should create transaction successfully with null description")
    void shouldCreateTransactionWithNullDescription() {
        // Given
        TransactionRequest request = buildValidRequestWithNullDescription();
        TransactionResponse expectedResponse = buildSuccessfulResponseWithNullDescription();

        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "null-desc-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TransactionResponse.class)
                .value(response -> assertThat(response.getDescription()).isNull());
    }

    @Test
    @DisplayName("Should fail when idempotency key header is missing")
    void shouldFailWhenIdempotencyKeyIsMissing() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();

        // When & Then
        // Note: Spring WebFlux doesn't validate required headers automatically,
        // resulting in 500 instead of 400 when header is missing
        webTestClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail when request body is missing")
    void shouldFailWhenRequestBodyIsMissing() {
        // When & Then
        // Note: Missing body results in 500 as Spring can't bind empty content
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when transaction value is missing")
    void shouldFailValidationWhenValueIsMissing() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value(null) // Missing required field
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("1234567890123456")
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when transaction value has invalid format")
    void shouldFailValidationWhenValueHasInvalidFormat() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("invalid-value") // Invalid format
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("1234567890123456")
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when payment method is missing")
    void shouldFailValidationWhenPaymentMethodIsMissing() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(null) // Missing required field
                .cardNumber("1234567890123456")
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when card number is missing")
    void shouldFailValidationWhenCardNumberIsMissing() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber(null) // Missing required field
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when card number has less than 4 digits")
    void shouldFailValidationWhenCardNumberIsTooShort() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("123") // Less than 4 digits
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when merchant name is missing")
    void shouldFailValidationWhenMerchantNameIsMissing() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("1234567890123456")
                .merchantName(null) // Missing required field
                .customerName("John Doe")
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should fail validation when customer name is missing")
    void shouldFailValidationWhenCustomerNameIsMissing() {
        // Given
        TransactionRequest request = TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("1234567890123456")
                .merchantName("Test Merchant")
                .customerName(null) // Missing required field
                .build();

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createTransactionHandler, never()).handle(any());
    }

    @Test
    @DisplayName("Should propagate TransactionCreationException as 500 Internal Server Error")
    void shouldPropagateTransactionCreationException() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.error(new TransactionCreationException("Database save failed")));

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "error-test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(createTransactionHandler, times(1)).handle(any());
    }

    @Test
    @DisplayName("Should propagate IdGenerationException as 503 Service Unavailable")
    void shouldPropagateIdGenerationException() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.error(new IdGenerationException("Numerator service unavailable")));

        // When & Then
        // GlobalErrorHandler maps IdGenerationException to 503 SERVICE_UNAVAILABLE
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "id-error-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503); // SERVICE_UNAVAILABLE
    }

    @Test
    @DisplayName("Should propagate InvalidTransactionException as 400 Bad Request")
    void shouldPropagateInvalidTransactionException() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.error(new InvalidTransactionException("Invalid card number format")));

        // When & Then
        // GlobalErrorHandler maps InvalidTransactionException to 400 BAD_REQUEST
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "invalid-test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should handle generic runtime exceptions as 500 Internal Server Error")
    void shouldHandleGenericRuntimeException() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "runtime-error-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("Should pass idempotency key correctly to handler")
    void shouldPassIdempotencyKeyCorrectly() {
        // Given
        String expectedKey = "unique-idempotency-key-123";
        TransactionRequest request = buildValidDebitCardRequest();
        TransactionResponse expectedResponse = buildSuccessfulDebitResponse();

        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", expectedKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Then
        verify(createTransactionHandler).handle(argThat(cmd ->
                cmd.getIdempotencyKey().equals(expectedKey)
        ));
    }

    @Test
    @DisplayName("Should create command with correct request data")
    void shouldCreateCommandWithCorrectRequestData() {
        // Given
        TransactionRequest request = buildValidDebitCardRequest();
        TransactionResponse expectedResponse = buildSuccessfulDebitResponse();

        when(createTransactionHandler.handle(any(CreateTransactionCommand.class)))
                .thenReturn(Mono.just(expectedResponse));

        // When
        webTestClient.post()
                .uri("/transactions")
                .header("Idempotency-Key", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Then
        verify(createTransactionHandler).handle(argThat(cmd -> {
            TransactionRequest req = cmd.getRequest();
            return req.getValue().equals("100.00") &&
                    req.getDescription().equals("Test payment") &&
                    req.getMethod() == PaymentMethod.DEBIT_CARD &&
                    req.getCardNumber().equals("1234567890123456") &&
                    req.getMerchantName().equals("Test Merchant") &&
                    req.getCustomerName().equals("John Doe");
        }));
    }

    // ==================== Helper Methods ====================

    private TransactionRequest buildValidDebitCardRequest() {
        return TransactionRequest.builder()
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("1234567890123456")
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .build();
    }

    private TransactionRequest buildValidCreditCardRequest() {
        return TransactionRequest.builder()
                .value("250.50")
                .description("Credit card payment")
                .method(PaymentMethod.CREDIT_CARD)
                .cardNumber("9876543210987654")
                .merchantName("Premium Store")
                .customerName("Jane Smith")
                .build();
    }

    private TransactionRequest buildValidRequestWithNullDescription() {
        return TransactionRequest.builder()
                .value("75.00")
                .description(null)
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("5555666677778888")
                .merchantName("Quick Shop")
                .customerName("Bob Johnson")
                .build();
    }

    private TransactionResponse buildSuccessfulDebitResponse() {
        return TransactionResponse.builder()
                .id("TXN-001")
                .value("100.00")
                .description("Test payment")
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("3456") // Last 4 digits
                .merchantName("Test Merchant")
                .customerName("John Doe")
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .receivable(ReceivableResponse.builder()
                        .id("REC-001")
                        .status("paid")
                        .createDate(LocalDateTime.now())
                        .paymentDate(LocalDateTime.now())
                        .subtotal("100.00")
                        .discount("2.00")
                        .total("98.00")
                        .build())
                .build();
    }

    private TransactionResponse buildSuccessfulCreditResponse() {
        return TransactionResponse.builder()
                .id("TXN-002")
                .value("250.50")
                .description("Credit card payment")
                .method(PaymentMethod.CREDIT_CARD)
                .cardNumber("7654")
                .merchantName("Premium Store")
                .customerName("Jane Smith")
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .receivable(ReceivableResponse.builder()
                        .id("REC-002")
                        .status("waiting_funds")
                        .createDate(LocalDateTime.now())
                        .paymentDate(LocalDateTime.now().plusDays(30))
                        .subtotal("250.50")
                        .discount("10.02")
                        .total("240.48")
                        .build())
                .build();
    }

    private TransactionResponse buildSuccessfulResponseWithNullDescription() {
        return TransactionResponse.builder()
                .id("TXN-003")
                .value("75.00")
                .description(null)
                .method(PaymentMethod.DEBIT_CARD)
                .cardNumber("8888")
                .merchantName("Quick Shop")
                .customerName("Bob Johnson")
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .receivable(ReceivableResponse.builder()
                        .id("REC-003")
                        .status("paid")
                        .createDate(LocalDateTime.now())
                        .paymentDate(LocalDateTime.now())
                        .subtotal("75.00")
                        .discount("1.50")
                        .total("73.50")
                        .build())
                .build();
    }
}
