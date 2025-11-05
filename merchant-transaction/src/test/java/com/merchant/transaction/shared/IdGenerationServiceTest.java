package com.merchant.transaction.shared;

import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.infrastructure.external.numerator.NumeratorClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdGenerationService Unit Tests")
class IdGenerationServiceTest {

    @Mock
    private NumeratorClient numeratorClient;

    @InjectMocks
    private IdGenerationService idGenerationService;

    @Nested
    @DisplayName("Successful ID Generation Scenarios")
    class SuccessfulIdGeneration {

        @Test
        @DisplayName("Should generate unique ID on first attempt when CAS succeeds")
        void shouldGenerateUniqueIdOnFirstAttempt() {
            // Arrange
            Long currentValue = 100L;
            Long expectedNextValue = 101L;

            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(currentValue));
            when(numeratorClient.testAndSet(currentValue, expectedNextValue))
                    .thenReturn(Mono.just(expectedNextValue));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("101")
                    .verifyComplete();

            verify(numeratorClient, times(1)).getCurrentValue();
            verify(numeratorClient, times(1)).testAndSet(currentValue, expectedNextValue);
        }

        @Test
        @DisplayName("Should generate unique ID after retry when initial CAS fails")
        void shouldGenerateIdAfterRetryWhenCasFails() {
            // Arrange
            Long firstAttempt = 100L;
            Long secondAttempt = 105L;
            Long expectedValue = 106L;

            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(firstAttempt))
                    .thenReturn(Mono.just(secondAttempt));

            when(numeratorClient.testAndSet(firstAttempt, firstAttempt + 1))
                    .thenReturn(Mono.just(-1L)); // CAS failure
            when(numeratorClient.testAndSet(secondAttempt, expectedValue))
                    .thenReturn(Mono.just(expectedValue)); // CAS success

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("106")
                    .verifyComplete();

            verify(numeratorClient, times(2)).getCurrentValue();
            verify(numeratorClient, times(2)).testAndSet(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should generate unique ID after multiple retries with exponential backoff")
        void shouldGenerateIdAfterMultipleRetriesWithBackoff() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L))
                    .thenReturn(Mono.just(101L))
                    .thenReturn(Mono.just(102L))
                    .thenReturn(Mono.just(103L));

            when(numeratorClient.testAndSet(anyLong(), anyLong()))
                    .thenReturn(Mono.just(-1L))  // Attempt 1: Fail
                    .thenReturn(Mono.just(-1L))  // Attempt 2: Fail
                    .thenReturn(Mono.just(-1L))  // Attempt 3: Fail
                    .thenReturn(Mono.just(104L)); // Attempt 4: Success

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("104")
                    .verifyComplete();

            verify(numeratorClient, times(4)).getCurrentValue();
            verify(numeratorClient, times(4)).testAndSet(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should generate ID starting from zero when current value is zero")
        void shouldGenerateIdFromZero() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(0L));
            when(numeratorClient.testAndSet(0L, 1L))
                    .thenReturn(Mono.just(1L));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("1")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle large numeric values correctly")
        void shouldHandleLargeNumericValues() {
            // Arrange
            Long largeValue = Long.MAX_VALUE - 1;
            Long expectedValue = Long.MAX_VALUE;

            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(largeValue));
            when(numeratorClient.testAndSet(largeValue, expectedValue))
                    .thenReturn(Mono.just(expectedValue));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext(String.valueOf(expectedValue))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Failure Scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("Should throw IdGenerationException when all retries exhausted")
        void shouldThrowExceptionWhenRetriesExhausted() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L));
            when(numeratorClient.testAndSet(anyLong(), anyLong()))
                    .thenReturn(Mono.just(-1L)); // Always fail CAS

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error instanceof IdGenerationException &&
                            error.getMessage().contains("Failed to generate unique ID after 5 attempts")
                    )
                    .verify();

            verify(numeratorClient, times(6)).getCurrentValue(); // Initial + 5 retries
            verify(numeratorClient, times(6)).testAndSet(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw IdGenerationException when timeout occurs")
        void shouldThrowExceptionWhenTimeoutOccurs() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.delay(Duration.ofSeconds(15)).thenReturn(100L));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error instanceof IdGenerationException &&
                            error.getMessage().contains("Numerator API error")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should wrap Numerator client errors in IdGenerationException")
        void shouldWrapNumeratorClientErrors() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error instanceof IdGenerationException &&
                            error.getMessage().contains("Numerator API error") &&
                            error.getMessage().contains("Connection refused")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should preserve IdGenerationException without wrapping")
        void shouldPreserveIdGenerationExceptionWithoutWrapping() {
            // Arrange
            IdGenerationException originalException = new IdGenerationException("Original error");
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.error(originalException));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error == originalException &&
                            error.getMessage().equals("Original error")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should throw exception when test-and-set returns error during retry")
        void shouldThrowExceptionWhenTestAndSetFails() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L));
            when(numeratorClient.testAndSet(anyLong(), anyLong()))
                    .thenReturn(Mono.error(new RuntimeException("Test-and-set failed")));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error instanceof IdGenerationException &&
                            error.getMessage().contains("Numerator API error") &&
                            error.getMessage().contains("Test-and-set failed")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("Reactive Behavior Verification")
    class ReactiveBehaviorVerification {

        @Test
        @DisplayName("Should execute lazily and only subscribe when requested")
        void shouldExecuteLazily() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L));
            when(numeratorClient.testAndSet(100L, 101L))
                    .thenReturn(Mono.just(101L));

            // Act - Create Mono but don't subscribe
            Mono<String> result = idGenerationService.generateUniqueId();

            // Assert - No interaction should happen yet
            verifyNoInteractions(numeratorClient);

            // Now subscribe
            StepVerifier.create(result)
                    .expectNext("101")
                    .verifyComplete();

            // Assert - Now interactions should have occurred
            verify(numeratorClient, times(1)).getCurrentValue();
            verify(numeratorClient, times(1)).testAndSet(100L, 101L);
        }

        @Test
        @DisplayName("Should maintain reactive chain without blocking")
        void shouldMaintainReactiveChainWithoutBlocking() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(200L).delayElement(Duration.ofMillis(50)));
            when(numeratorClient.testAndSet(200L, 201L))
                    .thenReturn(Mono.just(201L).delayElement(Duration.ofMillis(50)));

            // Act & Assert - Verify non-blocking behavior
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectSubscription()
                    .expectNoEvent(Duration.ofMillis(40))
                    .thenAwait(Duration.ofMillis(110))
                    .expectNext("201")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should propagate cancellation signal upstream")
        void shouldPropagateCancellationUpstream() {
            // Arrange
            lenient().when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L).delayElement(Duration.ofSeconds(5)));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectSubscription()
                    .thenCancel()
                    .verify();
        }

        @Test
        @DisplayName("Should return String representation of numeric ID")
        void shouldReturnStringRepresentation() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(12345L));
            when(numeratorClient.testAndSet(12345L, 12346L))
                    .thenReturn(Mono.just(12346L));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .assertNext(id -> {
                        assertThat(id).isInstanceOf(String.class);
                        assertThat(id).isEqualTo("12346");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Retry Logic and Backoff Verification")
    class RetryLogicVerification {

        @Test
        @DisplayName("Should apply exponential backoff between retries")
        void shouldApplyExponentialBackoff() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L))
                    .thenReturn(Mono.just(101L))
                    .thenReturn(Mono.just(102L));

            when(numeratorClient.testAndSet(anyLong(), anyLong()))
                    .thenReturn(Mono.just(-1L))
                    .thenReturn(Mono.just(-1L))
                    .thenReturn(Mono.just(103L));

            // Act & Assert - Verify with timing expectations
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectSubscription()
                    .thenAwait(Duration.ofMillis(50))  // Base delay ~100ms
                    .thenAwait(Duration.ofMillis(150)) // Second retry ~200ms
                    .expectNext("103")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should only retry on CAS failure exceptions")
        void shouldOnlyRetryOnCasFailure() {
            // Arrange - First call fails with CAS, second with different error
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L))
                    .thenReturn(Mono.just(101L));

            when(numeratorClient.testAndSet(100L, 101L))
                    .thenReturn(Mono.just(-1L)); // CAS failure - should retry
            when(numeratorClient.testAndSet(101L, 102L))
                    .thenReturn(Mono.error(new RuntimeException("Network error"))); // Non-CAS error

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectErrorMatches(error ->
                            error instanceof IdGenerationException &&
                            error.getMessage().contains("Network error")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should respect maximum retry limit of 5 attempts")
        void shouldRespectMaximumRetryLimit() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L));
            when(numeratorClient.testAndSet(anyLong(), anyLong()))
                    .thenReturn(Mono.just(-1L)); // Always fail

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectError(IdGenerationException.class)
                    .verify();

            // Verify exactly 6 calls (initial + 5 retries)
            verify(numeratorClient, times(6)).getCurrentValue();
            verify(numeratorClient, times(6)).testAndSet(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should complete with empty Mono when getCurrentValue returns empty")
        void shouldCompleteWithEmptyMono() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectComplete()
                    .verify();
        }

        @Test
        @DisplayName("Should handle negative current value")
        void shouldHandleNegativeCurrentValue() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(-1L));
            when(numeratorClient.testAndSet(-1L, 0L))
                    .thenReturn(Mono.just(0L));

            // Act & Assert
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("0")
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should complete within timeout period for successful operations")
        void shouldCompleteWithinTimeout() {
            // Arrange
            when(numeratorClient.getCurrentValue())
                    .thenReturn(Mono.just(100L).delayElement(Duration.ofSeconds(1)));
            when(numeratorClient.testAndSet(100L, 101L))
                    .thenReturn(Mono.just(101L));

            // Act & Assert - Should complete within 10 second timeout
            StepVerifier.create(idGenerationService.generateUniqueId())
                    .expectNext("101")
                    .expectComplete()
                    .verify(Duration.ofSeconds(3));
        }
    }
}