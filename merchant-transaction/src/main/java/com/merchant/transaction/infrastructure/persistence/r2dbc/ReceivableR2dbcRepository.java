package com.merchant.transaction.infrastructure.persistence.r2dbc;

import com.merchant.transaction.application.query.port.ReceivableQueryPort;
import com.merchant.transaction.infrastructure.persistence.entity.ReceivableEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * R2DBC repository for reactive receivable persistence.
 */
@Repository
public interface ReceivableR2dbcRepository extends ReactiveCrudRepository<ReceivableEntity, String> {

    @Query("SELECT * FROM receivables WHERE transaction_id = :transactionId")
    Mono<ReceivableEntity> findByTransactionId(String transactionId);

    /**
     * Finds all receivables created within the specified date range.
     *
     * @param startDate Start of the period (inclusive)
     * @param endDate End of the period (inclusive)
     * @return Flux of receivables within the date range
     */
    Flux<ReceivableEntity> findByCreateDateBetween(LocalDateTime startDate, LocalDateTime endDate);

}
