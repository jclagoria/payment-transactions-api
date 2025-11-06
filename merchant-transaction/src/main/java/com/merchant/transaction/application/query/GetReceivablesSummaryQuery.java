package com.merchant.transaction.application.query;

import com.merchant.transaction.application.query.dto.ReceivablesSummaryRequest;
import lombok.Value;

/**
 * Query command for retrieving receivables summary.
 * Follows CQRS pattern for read operations.
 */
@Value
public class GetReceivablesSummaryQuery {

    ReceivablesSummaryRequest request;

    public static GetReceivablesSummaryQuery of(ReceivablesSummaryRequest request) {
        request.validate();
        return new  GetReceivablesSummaryQuery(request);
    }
}
