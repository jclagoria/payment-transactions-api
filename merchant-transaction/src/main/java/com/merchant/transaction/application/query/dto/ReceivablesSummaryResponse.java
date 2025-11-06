package com.merchant.transaction.application.query.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for receivables summary aggregation.
 * Contains total receivables, future receivables, and fee breakdown.
 */
@Value
@Builder
public class ReceivablesSummaryResponse {

    /**
     * Total amount of all receivables in the period (sum of all receivable totals).
     */
    BigDecimal totalReceivables;

    /**
     * Amount receivable in the future (sum of waiting_funds receivables).
     */
    BigDecimal futureReceivables;

    /**
     * Total fee charged across all receivables (sum of all discounts).
     */
    BigDecimal totalFeeCharged;

    /**
     * Period information for the query.
     */
    PeriodInfo period;

    /**
     * Breakdown by receivable status.
     */
    StatusBreakdown breakdown;

    @Value
    @Builder
    public static class PeriodInfo {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Value
    @Builder
    public static class StatusBreakdown {
        ReceivableStats paid;
        ReceivableStats waitingFunds;
    }

    @Value
    @Builder
    public static class ReceivableStats {
        Long count;
        BigDecimal subtotal;
        BigDecimal discount;
        BigDecimal total;
    }

}