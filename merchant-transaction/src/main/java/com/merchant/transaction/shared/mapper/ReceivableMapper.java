package com.merchant.transaction.shared.mapper;

import com.merchant.transaction.domain.model.Money;
import com.merchant.transaction.domain.model.Receivable;
import com.merchant.transaction.infrastructure.persistence.entity.ReceivableEntity;
import org.springframework.stereotype.Component;

@Component
public class ReceivableMapper {

    public ReceivableEntity toEntity(Receivable domain) {
        return ReceivableEntity.builder()
                .id(domain.getId())
                .transactionId(domain.getTransactionId())
                .status(domain.getStatus())
                .createDate(domain.getCreateDate())
                .paymentDate(domain.getPaymentDate())
                .subTotal(domain.getSubtotal().getAmount())
                .discount(domain.getDiscount().getAmount())
                .total(domain.getTotal().getAmount())
                .build();
    }

    public Receivable toDomain(ReceivableEntity entity) {
        return Receivable.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .status(entity.getStatus())
                .createDate(entity.getCreateDate())
                .paymentDate(entity.getPaymentDate())
                .subtotal(new Money(entity.getSubTotal()))
                .discount(new Money(entity.getDiscount()))
                .total(new Money(entity.getTotal()))
                .build();
    }

}
