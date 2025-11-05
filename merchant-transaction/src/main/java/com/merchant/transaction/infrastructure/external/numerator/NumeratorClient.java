package com.merchant.transaction.infrastructure.external.numerator;

import reactor.core.publisher.Mono;

public interface NumeratorClient {

    /**
     * Gets the current numerator value.
     * @return Current value
     */
    Mono<Long> getCurrentValue();

    /**
     * Performs atomic test-and-set operation.
     * @param oldValue Expected current value
     * @param newValue New value to set
     * @return newValue if successful, -1 if CAS failed
     */
    Mono<Long> testAndSet(Long oldValue, Long newValue);

}
