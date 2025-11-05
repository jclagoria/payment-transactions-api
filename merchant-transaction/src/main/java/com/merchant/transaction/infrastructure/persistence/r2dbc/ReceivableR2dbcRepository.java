package com.merchant.transaction.infrastructure.persistence.r2dbc;

import com.merchant.transaction.infrastructure.persistence.entity.ReceivableEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ReceivableR2dbcRepository extends ReactiveCrudRepository<ReceivableEntity, String> {

    @Query("SELECT * FROM receivables WHERE transaction_id = :transactionId")
    Mono<ReceivableEntity> findByTransactionId(String transactionId);

}
