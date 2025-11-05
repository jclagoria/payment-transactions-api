package com.merchant.transaction.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class Receivable {

    String id;
    String transactionId;
    String status;
    LocalDateTime createDate;
    LocalDateTime paymentDate;
    Money subtotal;
    Money discount;
    Money total;

}
