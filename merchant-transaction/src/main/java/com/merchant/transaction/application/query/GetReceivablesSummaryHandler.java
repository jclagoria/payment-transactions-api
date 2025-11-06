package com.merchant.transaction.application.query;

import com.merchant.transaction.application.query.dto.ReceivableProjection;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryResponse;
import com.merchant.transaction.application.query.port.ReceivableQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Query handler for retrieving receivables summary.
 * Orchestrates the query execution and response composition.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetReceivablesSummaryHandler {

    private final ReceivableQueryPort receivableQueryPort;

    /**
     * Handles the receivables summary query.
     *
     * @param query The query containing date range parameters
     * @return Mono emitting the aggregated summary response
     */
    public Mono<ReceivablesSummaryResponse> handle(GetReceivablesSummaryQuery query) {
        log.info("Processing receivables summary query for period: {} to {}",
                query.getRequest().getStartDate(),
                query.getRequest().getEndDate());

        return receivableQueryPort.finByDateRange(
                        query.getRequest().getStartDate(),
                        query.getRequest().getEndDate()
                )
                .collectList()
                .map(receivables ->
                        buildSummaryResponse(receivables, query)
                );
    }

    private ReceivablesSummaryResponse buildSummaryResponse(
            List<ReceivableProjection> receivables,
            GetReceivablesSummaryQuery query
    ) {

        // Separate paid vs waiting_funds receivables
        List<ReceivableProjection> paidReceivables = receivables.stream()
                .filter(r -> "paid".equals(r.getStatus()))
                .toList();

        List<ReceivableProjection> waitingFundsReceivables = receivables.stream()
                .filter(r -> "waiting_funds".equals(r.getStatus()))
                .toList();

        BigDecimal totalReceivables = calculateTotal(receivables);
        BigDecimal futureReceivables = calculateSubtotal(waitingFundsReceivables);
        BigDecimal totalFeeCharged = calculateTotalFees(receivables);

        return ReceivablesSummaryResponse.builder()
                .totalReceivables(totalReceivables)
                .futureReceivables(futureReceivables)
                .totalFeeCharged(totalFeeCharged)
                .period(ReceivablesSummaryResponse.PeriodInfo.builder()
                        .startDate(query.getRequest().getStartDate())
                        .endDate(query.getRequest().getEndDate())
                        .build())
                .breakdown(ReceivablesSummaryResponse.StatusBreakdown.builder()
                        .paid(buildStats(paidReceivables))
                        .waitingFunds(buildStats(waitingFundsReceivables))
                        .build())
                .build();
    }

    private ReceivablesSummaryResponse.ReceivableStats buildStats(
            List<ReceivableProjection> receivables
    ) {
        return ReceivablesSummaryResponse.ReceivableStats.builder()
                .count((long) receivables.size())
                .subtotal(calculateSubtotal(receivables))
                .discount(calculateTotalFees(receivables))
                .total(calculateTotal(receivables))
                .build();
    }

    private BigDecimal calculateTotal(List<ReceivableProjection> receivables) {
        return receivables.stream()
                .map(ReceivableProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSubtotal(List<ReceivableProjection> receivables) {
        return receivables.stream()
                .map(ReceivableProjection::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalFees(List<ReceivableProjection> receivables) {
        return receivables.stream()
                .map(ReceivableProjection::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
