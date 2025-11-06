package com.merchant.transaction.application.query.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Request DTO for receivables summary query.
 * Defines the time period for receivables calculation.
 */
@Value
@Builder
public class ReceivablesSummaryRequest {

    @NotNull(message = "Start date is required")
    LocalDateTime startDate;

    @NotNull(message = "End date is required")
    LocalDateTime endDate;

    public void validate() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date must be after or equal to start date"
            );
        }
    }

}
