package com.merchant.transaction.shared;

import com.merchant.transaction.domain.exception.IdGenerationException;
import com.merchant.transaction.infrastructure.external.numerator.NumeratorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdGenerationService {

    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_DELAY = Duration.ofMillis(100);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final NumeratorClient numeratorClient;

    /**
     * Generates unique ID using atomic test-and-set with exponential backoff.
     *
     * @return Unique ID as String
     * @throws IdGenerationException if all retries exhausted or timeout occurs
     */
    public Mono<String> generateUniqueId() {
        return Mono.defer(() ->
                        numeratorClient.getCurrentValue()
                                .flatMap(currentValue -> {
                                    log.info("Generating unique id for transaction: {}", currentValue);
                                    Long nextValue = currentValue + 1;
                                    return numeratorClient.testAndSet(currentValue, nextValue)
                                            .flatMap(result -> {
                                                if (result != -1L) {
                                                    log.info("Generated unique ID: {}", result);
                                                    return Mono.just(String.valueOf(result));
                                                }
                                                // CAS failed - trigger retry
                                                return Mono.error(new CasFailureException());
                                            });
                                })
                )
                // Exponential backoff: 100ms, 200ms, 400ms, 800ms, 1600ms
                .retryWhen(Retry.backoff(MAX_RETRIES, BASE_DELAY)
                        .filter(throwable -> throwable instanceof CasFailureException)
                        .doBeforeRetry(signal ->
                                log.debug("Retrying ID generation, attempt {}", signal.totalRetries() + 1))
                        .onRetryExhaustedThrow((spec, signal) ->
                                new IdGenerationException(
                                        "Failed to generate unique ID after " + MAX_RETRIES + " attempts"))
                )
                .timeout(TIMEOUT)
                .onErrorMap(error -> {
                    if (error instanceof IdGenerationException) {
                        return error;
                    }
                    log.error("Failed to generate unique ID after retry", error);
                    return new IdGenerationException("Numerator API error: " + error.getMessage(), error);
                });
    }

    /**
     * Exception thrown when test-and-set CAS operation fails.
     * Signals retry logic to attempt again.
     */
    private static class CasFailureException extends RuntimeException {
        public CasFailureException() {
            super("Test-and-set CAS conflict");
        }
    }

}
