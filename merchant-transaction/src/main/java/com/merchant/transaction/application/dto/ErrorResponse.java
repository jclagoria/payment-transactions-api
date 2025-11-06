package com.merchant.transaction.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErrorResponse {

    LocalDateTime timestamp;
    int status;
    String error;
    String message;

}
