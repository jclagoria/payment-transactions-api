package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.model.Money;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Receivable;

public interface ReceivableDomainService {

    public Receivable calculateReceivable(
            String receivableId,
            String transactionId,
            Money transactionValue,
            PaymentMethod method
    );

}
