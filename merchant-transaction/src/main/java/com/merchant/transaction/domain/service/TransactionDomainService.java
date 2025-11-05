package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Transaction;

public interface TransactionDomainService {

    public Transaction createTransaction(
            String id,
            String value,
            String description,
            PaymentMethod method,
            String cardNumber,
            String merchantName,
            String customerName
    );

}
