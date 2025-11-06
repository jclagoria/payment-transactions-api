package com.merchant.transaction.application.query.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Query-specific projection for receivables summary aggregation.
 *
 * <p>This projection contains only the fields required for summary calculations,
 * providing a lightweight alternative to the full domain model (Receivable with Money value objects)
 * or infrastructure entity (ReceivableEntity).
 *
 * <p><b>CQRS Optimization:</b> Query-side operations can use specialized projections
 * that avoid unnecessary domain complexity.
 *
 * <p><b>Hexagonal Architecture:</b> This DTO belongs to the application layer and has no
 * dependencies on infrastructure concerns.
 *
 * @see com.merchant.transaction.application.query.GetReceivablesSummaryHandler
 * @see com.merchant.transaction.application.query.port.ReceivableQueryPort
 */
@Value
@Builder
public class ReceivableProjection {

    /**
     * Receivable subtotal amount before fees.
     */
    BigDecimal subtotal;

    /**
     * Fee/discount amount charged for the receivable.
     */
    BigDecimal discount;

    /**
     * Total receivable amount after fees (subtotal - discount).
     */
    BigDecimal total;

    /**
     * Receivable status: "paid" or "waiting_funds".
     */
    String status;

    /**
     * Receivable creation timestamp.
     */
    LocalDateTime createDate;
}
