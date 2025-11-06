package com.merchant.transaction.application.query.port;

import com.merchant.transaction.application.query.dto.ReceivableProjection;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * Finds receivables within a specified date range.
 *
 * <p>Returns a reactive stream of lightweight projections optimized for
 * aggregation operations (summary calculations).
 *
 * <p><b>Implementation Note:</b> The adapter implementation should:
 * <ul>
 *   <li>Use indexed queries on create_date for performance</li>
 *   <li>Map infrastructure entities to application-layer projections</li>
 *   <li>Return empty Flux if no results found (not null)</li>
 * </ul>
 *
 * @param startDate Start of the date range (inclusive)
 * @param endDate End of the date range (inclusive)
 * @return Flux emitting receivable projections within the date range (empty if none found)
 * @throws IllegalArgumentException if startDate or endDate is null
 * @throws IllegalArgumentException if endDate is before startDate
 */
public interface ReceivableQueryPort {
    Flux<ReceivableProjection> finByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
