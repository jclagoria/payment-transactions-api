package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.exception.InvalidTransactionException;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class TransactionDomainServiceImpl implements TransactionDomainService {

    @Override
    public Transaction createTransaction(
            String id,
            String value,
            String description,
            PaymentMethod method,
            String cardNumber,
            String merchantName,
            String customerName
    ) {

        validateTransactionValue(value);
        validateCardNumber(cardNumber);
        validateMerchantName(merchantName);
        validateCustomerName(customerName);

        Transaction transaction = Transaction.create(
                id, value, description, method, cardNumber, merchantName, customerName
        );

        log.info("Created transaction {} for merchant {} with value {}",
                id, merchantName, value);

        return transaction;
    }

    private void validateTransactionValue(String value) {
        try {
            BigDecimal amount = new BigDecimal(value);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Transaction value must be positive");
            }
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Invalid transaction value format");
        }
    }

    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            throw new InvalidTransactionException("Card number must have at least 4 digits");
        }
    }

    private void validateMerchantName(String merchantName) {
        if (merchantName == null || merchantName.isBlank()) {
            throw new InvalidTransactionException("Merchant name is required");
        }
    }

    private void validateCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new InvalidTransactionException("Customer name is required");
        }
    }

}
