package com.merchant.transaction.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
@With
public class Transaction {

    String id;
    Money value;
    String description;
    PaymentMethod paymentMethod;
    String cardNumber;
    String merchantName;
    String customerName;
    TransactionStatus status;
    LocalDateTime createdAt;

    public static Transaction create (
            String id,
            String value,
            String description,
            PaymentMethod method,
            String cardNumber,
            String merchantName,
            String customerName
    ) {
        return Transaction.builder()
                .id(id)
                .value(new Money(value))
                .description(description)
                .paymentMethod(method)
                .cardNumber(extractLastFourDigits(cardNumber))
                .merchantName(merchantName)
                .customerName(customerName)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public Transaction markAsCompleted () {
        return this.withStatus(TransactionStatus.COMPLETED);
    }

    public Transaction markAsFailed () {
        return this.withStatus(TransactionStatus.FAILED);
    }

    private static String extractLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            throw new IllegalArgumentException("Invalid card number");
        }

        return cardNumber.substring(cardNumber.length() - 4);
    }

}
