package com.merchant.transaction.domain.model;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Value
public class Money {

    BigDecimal amount;

    public Money(String amount) {
        this.amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
