package com.merchant.transaction.infrastructure.external.numerator;

import com.merchant.transaction.infrastructure.external.numerator.dto.NumericResponse;
import com.merchant.transaction.infrastructure.external.numerator.dto.TestAndSetRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveNumeratorClient implements NumeratorClient {

    private final WebClient webClient;

    @Value("${numerator.api.endpoints.get-current}")
    private String getCurrentEndPoint;

    @Value("${numerator.api.endpoints.test-and-set}")
    private String testAndSerEndPoint;

    @Override
    public Mono<Long> getCurrentValue() {
        return webClient.get()
                .uri(getCurrentEndPoint)
                .retrieve()
                .bodyToMono(NumericResponse.class)
                .map(NumericResponse::getNumerator)
                .doOnNext(value -> log.debug("Retrieved current numerator value: {}", value))
                .doOnError(error -> log.error("Error retrieving numerator value", error));
    }

    @Override
    public Mono<Long> testAndSet(Long oldValue, Long newValue) {
        TestAndSetRequest request = new TestAndSetRequest(oldValue, newValue);
        log.info("Test and set request: {} {}", request.getOldValue(), request.getNewValue());
        return webClient.put()
                .uri(testAndSerEndPoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NumericResponse.class)
                .map(NumericResponse::getNumerator)
                .doOnNext(result -> {
                    if (result != -1L) {
                        log.debug("Test-and-set successful: old={}, new={}, result={}",
                                oldValue, newValue, result);
                    } else {
                        log.debug("Test-and-set failed (CAS conflict): old={}, new={}",
                                oldValue, newValue);
                    }
                })
                .doOnError(error -> log.error("Error in test-and-set operation", error));
    }
}
