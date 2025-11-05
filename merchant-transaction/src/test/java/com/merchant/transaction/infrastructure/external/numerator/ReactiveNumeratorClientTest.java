package com.merchant.transaction.infrastructure.external.numerator;

import com.merchant.transaction.infrastructure.external.numerator.dto.NumericResponse;
import com.merchant.transaction.infrastructure.external.numerator.dto.TestAndSetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactiveNumeratorClient Unit Tests")
@SuppressWarnings("unchecked")
class ReactiveNumeratorClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ReactiveNumeratorClient reactiveNumeratorClient;

    private static final String GET_CURRENT_ENDPOINT = "/numerator";
    private static final String TEST_AND_SET_ENDPOINT = "/numerator/test-and-set";

    @BeforeEach
    void setUp() {
        reactiveNumeratorClient = new ReactiveNumeratorClient(webClient);
        ReflectionTestUtils.setField(reactiveNumeratorClient, "getCurrentEndPoint", GET_CURRENT_ENDPOINT);
        ReflectionTestUtils.setField(reactiveNumeratorClient, "testAndSerEndPoint", TEST_AND_SET_ENDPOINT);
    }

    @Nested
    @DisplayName("getCurrentValue() Tests")
    class GetCurrentValueTests {

        @Test
        @DisplayName("Should successfully retrieve current numerator value")
        void shouldRetrieveCurrentValueSuccessfully() {
            // Given
            Long expectedValue = 12345L;
            NumericResponse response = new NumericResponse(expectedValue);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectNext(expectedValue)
                    .verifyComplete();

            verify(webClient).get();
            verify(requestHeadersUriSpec).uri(GET_CURRENT_ENDPOINT);
            verify(responseSpec).bodyToMono(NumericResponse.class);
        }

        @Test
        @DisplayName("Should retrieve zero as current numerator value")
        void shouldRetrieveZeroValue() {
            // Given
            Long expectedValue = 0L;
            NumericResponse response = new NumericResponse(expectedValue);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectNext(expectedValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should retrieve large numerator value successfully")
        void shouldRetrieveLargeValue() {
            // Given
            Long expectedValue = Long.MAX_VALUE;
            NumericResponse response = new NumericResponse(expectedValue);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectNext(expectedValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle 404 error when retrieving current value")
        void shouldHandleNotFoundError() {
            // Given
            WebClientResponseException exception = WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    null,
                    null,
                    null
            );

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(exception));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectErrorMatches(throwable ->
                            throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().value() == 404)
                    .verify();
        }

        @Test
        @DisplayName("Should handle 500 server error when retrieving current value")
        void shouldHandleServerError() {
            // Given
            WebClientResponseException exception = WebClientResponseException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    null,
                    null,
                    null
            );

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(exception));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectErrorMatches(throwable ->
                            throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().value() == 500)
                    .verify();
        }

        @Test
        @DisplayName("Should handle timeout when retrieving current value")
        void shouldHandleTimeout() {
            // Given
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class))
                    .thenReturn(Mono.delay(Duration.ofSeconds(10))
                            .then(Mono.empty()));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectTimeout(Duration.ofSeconds(5))
                    .verify();
        }

        @Test
        @DisplayName("Should handle network connection error")
        void shouldHandleNetworkError() {
            // Given
            RuntimeException networkException = new RuntimeException("Connection refused");

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(networkException));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .expectErrorMessage("Connection refused")
                    .verify();
        }

        @Test
        @DisplayName("Should handle empty response body")
        void shouldHandleEmptyResponse() {
            // Given
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.getCurrentValue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("testAndSet() Tests")
    class TestAndSetTests {

        @Test
        @DisplayName("Should successfully perform test-and-set operation")
        void shouldPerformTestAndSetSuccessfully() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;
            NumericResponse response = new NumericResponse(newValue);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectNext(newValue)
                    .verifyComplete();

            verify(webClient).put();
            verify(requestBodyUriSpec).uri(TEST_AND_SET_ENDPOINT);
            verify(requestBodySpec).bodyValue(any(TestAndSetRequest.class));
        }

        @Test
        @DisplayName("Should handle CAS conflict with -1 response")
        void shouldHandleCasConflict() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;
            Long conflictResponse = -1L;
            NumericResponse response = new NumericResponse(conflictResponse);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectNext(conflictResponse)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should send correct request body for test-and-set")
        void shouldSendCorrectRequestBody() {
            // Given
            Long oldValue = 50L;
            Long newValue = 51L;
            NumericResponse response = new NumericResponse(newValue);

            ArgumentCaptor<TestAndSetRequest> requestCaptor = ArgumentCaptor.forClass(TestAndSetRequest.class);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(requestCaptor.capture())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectNext(newValue)
                    .verifyComplete();

            // Then
            TestAndSetRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getOldValue()).isEqualTo(oldValue);
            assertThat(capturedRequest.getNewValue()).isEqualTo(newValue);
        }

        @Test
        @DisplayName("Should handle test-and-set with zero values")
        void shouldHandleZeroValues() {
            // Given
            Long oldValue = 0L;
            Long newValue = 1L;
            NumericResponse response = new NumericResponse(newValue);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectNext(newValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle test-and-set with large values")
        void shouldHandleLargeValues() {
            // Given
            Long oldValue = Long.MAX_VALUE - 1;
            Long newValue = Long.MAX_VALUE;
            NumericResponse response = new NumericResponse(newValue);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.just(response));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectNext(newValue)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle 409 conflict error in test-and-set")
        void shouldHandleConflictError() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;
            WebClientResponseException exception = WebClientResponseException.create(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    null,
                    null,
                    null
            );

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(exception));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectErrorMatches(throwable ->
                            throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().value() == 409)
                    .verify();
        }

        @Test
        @DisplayName("Should handle 500 server error in test-and-set")
        void shouldHandleServerErrorInTestAndSet() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;
            WebClientResponseException exception = WebClientResponseException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    null,
                    null,
                    null
            );

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(exception));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectErrorMatches(throwable ->
                            throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().value() == 500)
                    .verify();
        }

        @Test
        @DisplayName("Should handle timeout in test-and-set operation")
        void shouldHandleTimeoutInTestAndSet() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class))
                    .thenReturn(Mono.delay(Duration.ofSeconds(10))
                            .then(Mono.empty()));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectTimeout(Duration.ofSeconds(5))
                    .verify();
        }

        @Test
        @DisplayName("Should handle network error in test-and-set")
        void shouldHandleNetworkErrorInTestAndSet() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;
            RuntimeException networkException = new RuntimeException("Connection timeout");

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.error(networkException));

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .expectErrorMessage("Connection timeout")
                    .verify();
        }

        @Test
        @DisplayName("Should handle empty response in test-and-set")
        void shouldHandleEmptyResponseInTestAndSet() {
            // Given
            Long oldValue = 100L;
            Long newValue = 101L;

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle multiple concurrent test-and-set operations")
        void shouldHandleConcurrentOperations() {
            // Given
            Long oldValue = 100L;
            Long newValue1 = 101L;
            Long newValue2 = 102L;
            NumericResponse successResponse = new NumericResponse(newValue1);
            NumericResponse conflictResponse = new NumericResponse(-1L);

            when(webClient.put()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any(TestAndSetRequest.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(NumericResponse.class))
                    .thenReturn(Mono.just(successResponse))
                    .thenReturn(Mono.just(conflictResponse));

            // When & Then - First call succeeds
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue1))
                    .expectNext(newValue1)
                    .verifyComplete();

            // Second call fails with CAS conflict
            StepVerifier.create(reactiveNumeratorClient.testAndSet(oldValue, newValue2))
                    .expectNext(-1L)
                    .verifyComplete();

            verify(webClient, times(2)).put();
        }
    }
}
