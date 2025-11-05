package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.model.Money;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Receivable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ReceivableDomainServiceImpl implements ReceivableDomainService {

    private static final BigDecimal DEBIT_FEE_RATE = BigDecimal.valueOf(0.02);
    private static final BigDecimal CREDIT_FEE_RATE = BigDecimal.valueOf(0.04);
    private static final int CREDIT_PAYMENT_DAYS = 30;


    @Override
    public Receivable calculateReceivable(
            String receivableId,
            String transactionId,
            Money transactionValue,
            PaymentMethod method
    ) {

        BigDecimal feeRate = (method == PaymentMethod.DEBIT_CARD)
                ? DEBIT_FEE_RATE
                : CREDIT_FEE_RATE;

        Money discount = transactionValue.multiply(feeRate);

        Money total = transactionValue.subtract(discount);

        String status;
        LocalDateTime paymentDate;

        if (method == PaymentMethod.DEBIT_CARD) {
            status = "paid";
            paymentDate = LocalDateTime.now();
        } else {
            status = "waiting_funds";
            paymentDate = LocalDateTime.now().plusDays(CREDIT_PAYMENT_DAYS);
        }

        log.info("Calculated receivable: subtotal={}, discount={}, total={}, status={}",
                transactionValue, discount, total, status);

        return Receivable.builder()
                .id(receivableId)
                .transactionId(transactionId)
                .status(status)
                .createDate(LocalDateTime.now())
                .paymentDate(paymentDate)
                .discount(discount)
                .subtotal(transactionValue)
                .total(total)
                .build();
    }
}
