package com.merchant.transaction.application.command.dto;

import com.merchant.transaction.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "Transaction value is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Invalid value format")
    private String value;

    private String description;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{4,}$", message = "Card number must have at least 4 digits")
    private String cardNumber;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Customer name is required")
    private String customerName;

}
