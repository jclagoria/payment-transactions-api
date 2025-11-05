package com.merchant.transaction.application.command.dto;

import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.TransactionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TransactionResponse {

    String id;
    String value;
    String description;
    PaymentMethod method;
    String cardNumber;
    String merchantName;
    String customerName;
    TransactionStatus status;
    LocalDateTime createdAt;
    ReceivableResponse receivable;

}
