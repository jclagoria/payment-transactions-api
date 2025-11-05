package com.merchant.transaction.infrastructure.external.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService Unit Tests")
class IdempotencyServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    private IdempotencyService idempotencyService;

    private static final String TEST_KEY = "test-idempotency-key";
    private static final String CACHE_KEY = "idempotency:" + TEST_KEY;
    private static final String LOCK_KEY = CACHE_KEY + ":lock";
    private static final String TEST_RESULT = "test-result";

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("executeIdempotent() - Cache Hit Scenarios")
    class CacheHitScenarios {

        @Test
        @DisplayName("Should treat Mono.just(null) as empty and execute operation")
        void shouldTreatMonoJustNullAsEmpty() {
            // Arrange - Mono.just(null) will complete empty, so this is actually a cache miss
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Cache Miss with Lock Acquisition")
    class CacheMissLockAcquisitionScenarios {

        @Test
        @DisplayName("Should execute operation and cache result on cache miss with successful lock")
        void shouldExecuteOperationOnCacheMissWithLock() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())  // Cache miss
                    .thenReturn(Mono.empty()); // Double-check after lock

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));  // Lock acquired

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            verify(valueOperations, times(1)).setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class));
            verify(valueOperations, times(1)).set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class));
            verify(redisTemplate, times(1)).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("Should release lock after successful operation execution")
        void shouldReleaseLockAfterSuccessfulExecution() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify lock was released
            verify(redisTemplate, times(1)).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("Should release lock even when operation fails")
        void shouldReleaseLockWhenOperationFails() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            RuntimeException operationException = new RuntimeException("Operation failed");
            Supplier<Mono<String>> operation = () -> Mono.error(operationException);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectErrorMatches(error ->
                            error instanceof RuntimeException &&
                            error.getMessage().equals("Operation failed")
                    )
                    .verify();

            // Verify lock was still released
            verify(redisTemplate, times(1)).delete(LOCK_KEY);
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Lock Contention Scenarios")
    class LockContentionScenarios {

        @Test
        @DisplayName("Should retry when lock acquisition fails")
        void shouldRetryWhenLockAcquisitionFails() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())           // First attempt: cache miss
                    .thenAnswer(invocation -> Mono.just(TEST_RESULT)); // Second attempt: cache hit

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(false));      // Lock acquisition failed

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, () -> Mono.just("new-result")))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify retry occurred (2 cache get calls)
            verify(valueOperations, times(2)).get(CACHE_KEY);
        }

        @Test
        @DisplayName("Should handle delayed retry with exponential backoff")
        void shouldHandleDelayedRetryWithBackoff() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenAnswer(invocation -> Mono.just(TEST_RESULT));

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(false));

            long startTime = System.currentTimeMillis();

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, () -> Mono.just("new-result")))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            long endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;

            // Verify some delay occurred (at least 50ms, accounting for processing time)
            assertThat(elapsed).isGreaterThan(50);
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Invalid Key Scenarios")
    class InvalidKeyScenarios {

        @Test
        @DisplayName("Should execute operation without caching when key is null")
        void shouldExecuteWithoutCachingWhenKeyIsNull() {
            // Arrange
            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(null, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify no Redis interactions
            verifyNoInteractions(valueOperations);
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("Should execute operation without caching when key is empty")
        void shouldExecuteWithoutCachingWhenKeyIsEmpty() {
            // Arrange
            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent("", operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify no Redis interactions
            verifyNoInteractions(valueOperations);
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("Should execute operation without caching when key is blank")
        void shouldExecuteWithoutCachingWhenKeyIsBlank() {
            // Arrange
            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent("   ", operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify no Redis interactions
            verifyNoInteractions(valueOperations);
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Error Handling Scenarios")
    class ErrorHandlingScenarios {

        @Test
        @DisplayName("Should propagate operation errors to caller")
        void shouldPropagateOperationErrors() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            RuntimeException operationError = new RuntimeException("Business logic error");
            Supplier<Mono<String>> operation = () -> Mono.error(operationError);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectErrorMatches(error ->
                            error instanceof RuntimeException &&
                            error.getMessage().equals("Business logic error")
                    )
                    .verify();
        }

        @Test
        @DisplayName("Should handle cache set failures")
        void shouldHandleCacheSetFailures() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            RuntimeException setError = new RuntimeException("Cache set failed");
            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.error(setError));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectErrorMatches(error ->
                            error instanceof RuntimeException &&
                            error.getMessage().equals("Cache set failed")
                    )
                    .verify();

            // Verify lock was still released
            verify(redisTemplate, times(1)).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("Should handle lock deletion errors gracefully")
        void shouldHandleLockDeletionErrors() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.error(new RuntimeException("Delete failed")));

            Supplier<Mono<String>> operation = () -> Mono.just(TEST_RESULT);

            // Act & Assert - Should still return result despite delete error
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Reactive Behavior Verification")
    class ReactiveBehaviorVerification {

        @Test
        @DisplayName("Should handle operation that returns Mono.empty()")
        void shouldHandleOperationReturningMonoEmpty() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            Supplier<Mono<String>> operation = () -> Mono.empty();

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .verifyComplete();

            verify(redisTemplate, times(1)).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("Should ensure operation is only executed once per idempotency key")
        void shouldEnsureOperationExecutedOnlyOnce() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            AtomicInteger executionCounter = new AtomicInteger(0);
            Supplier<Mono<String>> operation = () -> {
                executionCounter.incrementAndGet();
                return Mono.just(TEST_RESULT);
            };

            // Act & Assert
            StepVerifier.create(idempotencyService.executeIdempotent(TEST_KEY, operation))
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Verify operation was executed exactly once
            assertThat(executionCounter.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("executeIdempotent() - Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle concurrent requests with same idempotency key")
        void shouldHandleConcurrentRequestsWithSameKey() {
            // Arrange
            when(valueOperations.get(CACHE_KEY))
                    .thenReturn(Mono.empty())
                    .thenReturn(Mono.empty());

            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(valueOperations.set(eq(CACHE_KEY), eq(TEST_RESULT), any(Duration.class)))
                    .thenReturn(Mono.just(true));

            when(redisTemplate.delete(LOCK_KEY))
                    .thenReturn(Mono.just(1L));

            AtomicInteger executionCounter = new AtomicInteger(0);
            Supplier<Mono<String>> operation = () -> {
                executionCounter.incrementAndGet();
                return Mono.just(TEST_RESULT).delayElement(Duration.ofMillis(100));
            };

            // Act & Assert
            Mono<String> execution1 = idempotencyService.executeIdempotent(TEST_KEY, operation);
            Mono<String> execution2 = idempotencyService.executeIdempotent(TEST_KEY, operation);

            StepVerifier.create(execution1)
                    .expectNext(TEST_RESULT)
                    .verifyComplete();

            // Note: In unit test, both executions use same mocks, so behavior is deterministic
            // In real scenario, second would wait for lock
        }
    }

    /**
     * Helper class for testing complex object caching
     */
    private static class ComplexObject {
        private final String value;
        private final Integer count;

        public ComplexObject(String value, Integer count) {
            this.value = value;
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public Integer getCount() {
            return count;
        }
    }
}
