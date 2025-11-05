package com.merchant.transaction.shared.mapper;

import com.merchant.transaction.domain.model.Transaction;
import com.merchant.transaction.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity toEntity(Transaction domain) {
        return TransactionEntity.builder()
                .id(domain.getId())
                .value(domain.getValue().getAmount())
                .description(domain.getDescription())
                .method(domain.getPaymentMethod())
                .cardNumber(domain.getCardNumber())
                .merchantName(domain.getMerchantName())
                .customerName(domain.getCustomerName())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .value(new com.merchant.transaction.domain.model.Money(entity.getValue().toString()))
                .description(entity.getDescription())
                .paymentMethod(entity.getMethod())
                .cardNumber(entity.getCardNumber())
                .merchantName(entity.getMerchantName())
                .customerName(entity.getCustomerName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
