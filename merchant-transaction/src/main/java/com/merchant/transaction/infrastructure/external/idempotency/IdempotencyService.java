package com.merchant.transaction.infrastructure.external.idempotency;

import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * Executes operation idempotently using distributed locking.
     *
     * @param idempotencyKey Unique key for the operation
     * @param operation Operation to execute if not cached
     * @return Cached result or newly executed result
     */
    public<T> Mono<T> executeIdempotent(String idempotencyKey, Supplier<Mono<T>> operation) {
        if(idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("No idempotency key provided - executing without caching");
            return operation.get();
        }

        String cacheKey = "idempotency:" + idempotencyKey;
        String lockKey = cacheKey + ":lock";

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(Object.class)
                .map(cached -> {
                    log.info("Idempotent request detected - returning cached response for key: {}",
                            idempotencyKey);
                    return (T) cached;
                })
                .switchIfEmpty(
                        // Cache miss - try to acquire lock and execute
                        acquireLockAndExecute(cacheKey, lockKey, idempotencyKey, operation)
                );
    }

    private <T> Mono<T> acquireLockAndExecute(
            String cacheKey,
            String lockKey,
            String idempotencyKey,
            Supplier<Mono<T>> operation
    ) {
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL)
                .flatMap(lockAcquired -> {
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        log.info("Lock acquired for idempotency key: {}", idempotencyKey);

                        return redisTemplate.opsForValue().get(cacheKey)
                                .cast(Object.class)
                                .map(cached -> (T) cached)
                                .switchIfEmpty(
                                        operation.get()
                                                .flatMap(result ->
                                                    redisTemplate.opsForValue()
                                                            .set(cacheKey, result, CACHE_TTL)
                                                            .thenReturn(result)
                                                        )
                                )
                                .doFinally(signal -> {
                                   redisTemplate.delete(lockKey).subscribe();
                                    log.info("Lock released for idempotency key: {}", idempotencyKey);
                                });

                    } else {
                        // Lock held by another instance - wait and retry
                        log.warn("Lock contention for idempotency key: {}, retrying...",
                                idempotencyKey);
                        return Mono.delay(RETRY_DELAY)
                                .then(executeIdempotent(idempotencyKey, operation));
                    }
                });
    }

}
