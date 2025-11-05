package com.merchant.transaction.application.command.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ReceivableResponse {

    String id;
    String status;
    LocalDateTime createDate;
    LocalDateTime paymentDate;
    String subtotal;
    String discount;
    String total;

}
