package com.merchant.transaction.infrastructure.persistence.r2dbc;

import com.merchant.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionR2dbcRepository extends ReactiveCrudRepository<TransactionEntity, String> {
}
