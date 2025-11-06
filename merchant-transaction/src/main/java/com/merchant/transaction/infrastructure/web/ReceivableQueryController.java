package com.merchant.transaction.infrastructure.web;

import com.merchant.transaction.application.query.GetReceivablesSummaryHandler;
import com.merchant.transaction.application.query.GetReceivablesSummaryQuery;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryRequest;
import com.merchant.transaction.application.query.dto.ReceivablesSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * REST controller for receivable query operations.
 * Follows CQRS pattern - read-only operations.
 */
@RestController
@RequestMapping("/receivables")
@RequiredArgsConstructor
@Slf4j
public class ReceivableQueryController {

    private final GetReceivablesSummaryHandler summaryHandler;

    /**
     * Calculates total receivables for a given period.
     *
     * Example: GET /receivables/summary?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
     *
     * @param startDate Start of the period (ISO-8601 format)
     * @param endDate End of the period (ISO-8601 format)
     * @return Mono emitting the receivables summary
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ReceivablesSummaryResponse> getReceivablesSummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate
    ) {
        log.info("Received receivables  request: startDate={}, endDate={}", startDate, endDate);

        ReceivablesSummaryRequest request = ReceivablesSummaryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        GetReceivablesSummaryQuery query = GetReceivablesSummaryQuery.of(request);

        return summaryHandler.handle(query)
                .doOnSuccess(response ->
                        log.info("Receivables summary calculated successfully: totalReceivables={}",
                                response.getTotalReceivables()))
                .doOnError(error -> log.error("Error calculating receivables summary", error));
    }

}
