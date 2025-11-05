package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.model.Money;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Receivable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ReceivableDomainServiceImpl Unit Tests")
class ReceivableDomainServiceImplTest {

    private ReceivableDomainServiceImpl receivableDomainService;

    @BeforeEach
    void setUp() {
        receivableDomainService = new ReceivableDomainServiceImpl();
    }

    @Nested
    @DisplayName("Debit Card Fee Calculation Scenarios")
    class DebitCardFeeCalculation {

        @Test
        @DisplayName("Should calculate receivable with 2% fee for debit card transaction")
        void shouldCalculateReceivableWith2PercentFeeForDebitCard() {
            // Arrange
            String receivableId = "RCV-001";
            String transactionId = "TXN-001";
            Money transactionValue = new Money("100.00");
            PaymentMethod method = PaymentMethod.DEBIT_CARD;

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    receivableId, transactionId, transactionValue, method
            );

            // Assert
            assertThat(receivable.getId()).isEqualTo(receivableId);
            assertThat(receivable.getTransactionId()).isEqualTo(transactionId);
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("2.00"); // 2% of 100
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("98.00"); // 100 - 2
            assertThat(receivable.getStatus()).isEqualTo("paid");
            assertThat(receivable.getCreateDate()).isNotNull();
            assertThat(receivable.getPaymentDate()).isNotNull();
        }

        @Test
        @DisplayName("Should set payment date to current date for debit card (D+0)")
        void shouldSetPaymentDateToCurrentDateForDebitCard() {
            // Arrange
            String receivableId = "RCV-002";
            String transactionId = "TXN-002";
            Money transactionValue = new Money("50.00");
            PaymentMethod method = PaymentMethod.DEBIT_CARD;
            LocalDateTime before = LocalDateTime.now();

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    receivableId, transactionId, transactionValue, method
            );

            LocalDateTime after = LocalDateTime.now();

            // Assert
            assertThat(receivable.getPaymentDate()).isNotNull();
            assertThat(receivable.getPaymentDate()).isBetween(before, after);
            assertThat(receivable.getStatus()).isEqualTo("paid");
        }

        @Test
        @DisplayName("Should set status to 'paid' for debit card transactions")
        void shouldSetStatusToPaidForDebitCard() {
            // Arrange
            Money transactionValue = new Money("75.50");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-003", "TXN-003", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getStatus()).isEqualTo("paid");
        }

        @Test
        @DisplayName("Should calculate correct fee for large debit card amount")
        void shouldCalculateCorrectFeeForLargeDebitCardAmount() {
            // Arrange
            Money transactionValue = new Money("10000.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-004", "TXN-004", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("200.00"); // 2% of 10000
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("9800.00"); // 10000 - 200
        }

        @Test
        @DisplayName("Should calculate correct fee for small debit card amount")
        void shouldCalculateCorrectFeeForSmallDebitCardAmount() {
            // Arrange
            Money transactionValue = new Money("1.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-005", "TXN-005", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("0.02"); // 2% of 1.00
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("0.98"); // 1.00 - 0.02
        }

        @Test
        @DisplayName("Should calculate fee with proper rounding for debit card")
        void shouldCalculateFeeWithProperRoundingForDebitCard() {
            // Arrange
            Money transactionValue = new Money("123.45");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-006", "TXN-006", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("2.47"); // 2% of 123.45 = 2.469 → 2.47
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("120.98"); // 123.45 - 2.47
        }
    }

    @Nested
    @DisplayName("Credit Card Fee Calculation Scenarios")
    class CreditCardFeeCalculation {

        @Test
        @DisplayName("Should calculate receivable with 4% fee for credit card transaction")
        void shouldCalculateReceivableWith4PercentFeeForCreditCard() {
            // Arrange
            String receivableId = "RCV-007";
            String transactionId = "TXN-007";
            Money transactionValue = new Money("100.00");
            PaymentMethod method = PaymentMethod.CREDIT_CARD;

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    receivableId, transactionId, transactionValue, method
            );

            // Assert
            assertThat(receivable.getId()).isEqualTo(receivableId);
            assertThat(receivable.getTransactionId()).isEqualTo(transactionId);
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("4.00"); // 4% of 100
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("96.00"); // 100 - 4
            assertThat(receivable.getStatus()).isEqualTo("waiting_funds");
            assertThat(receivable.getCreateDate()).isNotNull();
            assertThat(receivable.getPaymentDate()).isNotNull();
        }

        @Test
        @DisplayName("Should set payment date to D+30 for credit card")
        void shouldSetPaymentDateToPlus30DaysForCreditCard() {
            // Arrange
            String receivableId = "RCV-008";
            String transactionId = "TXN-008";
            Money transactionValue = new Money("200.00");
            PaymentMethod method = PaymentMethod.CREDIT_CARD;
            LocalDateTime expectedPaymentDate = LocalDateTime.now().plusDays(30);

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    receivableId, transactionId, transactionValue, method
            );

            // Assert
            assertThat(receivable.getPaymentDate()).isNotNull();
            assertThat(receivable.getPaymentDate())
                    .isCloseTo(expectedPaymentDate, within(5, ChronoUnit.SECONDS));
            assertThat(receivable.getStatus()).isEqualTo("waiting_funds");
        }

        @Test
        @DisplayName("Should set status to 'waiting_funds' for credit card transactions")
        void shouldSetStatusToWaitingFundsForCreditCard() {
            // Arrange
            Money transactionValue = new Money("150.75");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-009", "TXN-009", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getStatus()).isEqualTo("waiting_funds");
        }

        @Test
        @DisplayName("Should calculate correct fee for large credit card amount")
        void shouldCalculateCorrectFeeForLargeCreditCardAmount() {
            // Arrange
            Money transactionValue = new Money("5000.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-010", "TXN-010", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("200.00"); // 4% of 5000
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("4800.00"); // 5000 - 200
        }

        @Test
        @DisplayName("Should calculate correct fee for small credit card amount")
        void shouldCalculateCorrectFeeForSmallCreditCardAmount() {
            // Arrange
            Money transactionValue = new Money("1.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-011", "TXN-011", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("0.04"); // 4% of 1.00
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("0.96"); // 1.00 - 0.04
        }

        @Test
        @DisplayName("Should calculate fee with proper rounding for credit card")
        void shouldCalculateFeeWithProperRoundingForCreditCard() {
            // Arrange
            Money transactionValue = new Money("123.45");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-012", "TXN-012", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("4.94"); // 4% of 123.45 = 4.938 → 4.94
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("118.51"); // 123.45 - 4.94
        }
    }

    @Nested
    @DisplayName("Fee Rate Comparison Scenarios")
    class FeeRateComparison {

        @Test
        @DisplayName("Should apply lower fee rate for debit card than credit card")
        void shouldApplyLowerFeeRateForDebitCardThanCreditCard() {
            // Arrange
            Money transactionValue = new Money("1000.00");

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-DEBIT", "TXN-DEBIT", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-CREDIT", "TXN-CREDIT", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Debit fee should be less than credit fee
            assertThat(debitReceivable.getDiscount().getAmount())
                    .isLessThan(creditReceivable.getDiscount().getAmount());

            // Debit: 2% = 20.00, Credit: 4% = 40.00
            assertThat(debitReceivable.getDiscount().getAmount()).isEqualByComparingTo("20.00");
            assertThat(creditReceivable.getDiscount().getAmount()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("Should result in higher total for debit card than credit card")
        void shouldResultInHigherTotalForDebitCardThanCreditCard() {
            // Arrange
            Money transactionValue = new Money("500.00");

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-DEBIT-2", "TXN-DEBIT-2", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-CREDIT-2", "TXN-CREDIT-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Debit total should be greater than credit total (lower fee)
            assertThat(debitReceivable.getTotal().getAmount())
                    .isGreaterThan(creditReceivable.getTotal().getAmount());

            // Debit: 500 - 10 = 490, Credit: 500 - 20 = 480
            assertThat(debitReceivable.getTotal().getAmount()).isEqualByComparingTo("490.00");
            assertThat(creditReceivable.getTotal().getAmount()).isEqualByComparingTo("480.00");
        }

        @Test
        @DisplayName("Should calculate credit card fee as exactly double of debit card fee")
        void shouldCalculateCreditCardFeeAsDoubleOfDebitCardFee() {
            // Arrange
            Money transactionValue = new Money("250.00");

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-DEBIT-3", "TXN-DEBIT-3", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-CREDIT-3", "TXN-CREDIT-3", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Credit fee should be exactly 2x debit fee (4% vs 2%)
            BigDecimal debitFee = debitReceivable.getDiscount().getAmount();
            BigDecimal creditFee = creditReceivable.getDiscount().getAmount();

            assertThat(creditFee).isEqualByComparingTo(debitFee.multiply(BigDecimal.valueOf(2)));
            assertThat(debitFee).isEqualByComparingTo("5.00"); // 2% of 250
            assertThat(creditFee).isEqualByComparingTo("10.00"); // 4% of 250
        }
    }

    @Nested
    @DisplayName("Business Rules Validation Scenarios")
    class BusinessRulesValidation {

        @Test
        @DisplayName("Should follow business rule: Debit card = 2% fee, paid status, D+0")
        void shouldFollowBusinessRuleForDebitCard() {
            // Arrange
            Money transactionValue = new Money("300.00");
            LocalDateTime before = LocalDateTime.now();

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-BR-1", "TXN-BR-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            LocalDateTime after = LocalDateTime.now();

            // Assert - Business rule compliance
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("6.00"); // 2% fee
            assertThat(receivable.getStatus()).isEqualTo("paid"); // Status = paid
            assertThat(receivable.getPaymentDate()).isBetween(before, after); // D+0
        }

        @Test
        @DisplayName("Should follow business rule: Credit card = 4% fee, waiting_funds status, D+30")
        void shouldFollowBusinessRuleForCreditCard() {
            // Arrange
            Money transactionValue = new Money("300.00");
            LocalDateTime expectedPaymentDate = LocalDateTime.now().plusDays(30);

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-BR-2", "TXN-BR-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Business rule compliance
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("12.00"); // 4% fee
            assertThat(receivable.getStatus()).isEqualTo("waiting_funds"); // Status = waiting_funds
            assertThat(receivable.getPaymentDate())
                    .isCloseTo(expectedPaymentDate, within(5, ChronoUnit.SECONDS)); // D+30
        }

        @Test
        @DisplayName("Should always set create date to current timestamp")
        void shouldAlwaysSetCreateDateToCurrentTimestamp() {
            // Arrange
            Money transactionValue = new Money("100.00");
            LocalDateTime before = LocalDateTime.now();

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-CD-1", "TXN-CD-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-CD-2", "TXN-CD-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            LocalDateTime after = LocalDateTime.now();

            // Assert
            assertThat(debitReceivable.getCreateDate()).isBetween(before, after);
            assertThat(creditReceivable.getCreateDate()).isBetween(before, after);
        }

        @Test
        @DisplayName("Should preserve receivable ID and transaction ID in result")
        void shouldPreserveReceivableIdAndTransactionId() {
            // Arrange
            String receivableId = "RCV-PRESERVE-001";
            String transactionId = "TXN-PRESERVE-001";
            Money transactionValue = new Money("100.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    receivableId, transactionId, transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getId()).isEqualTo(receivableId);
            assertThat(receivable.getTransactionId()).isEqualTo(transactionId);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle minimum transaction value (0.01) for debit card")
        void shouldHandleMinimumTransactionValueForDebitCard() {
            // Arrange
            Money transactionValue = new Money("0.01");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-MIN-1", "TXN-MIN-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("0.00"); // 2% of 0.01 = 0.0002 → 0.00
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("0.01"); // 0.01 - 0.00
        }

        @Test
        @DisplayName("Should handle minimum transaction value (0.01) for credit card")
        void shouldHandleMinimumTransactionValueForCreditCard() {
            // Arrange
            Money transactionValue = new Money("0.01");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-MIN-2", "TXN-MIN-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("0.00"); // 4% of 0.01 = 0.0004 → 0.00
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("0.01"); // 0.01 - 0.00
        }

        @Test
        @DisplayName("Should handle maximum reasonable transaction value for debit card")
        void shouldHandleMaximumReasonableTransactionValueForDebitCard() {
            // Arrange
            Money transactionValue = new Money("999999.99");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-MAX-1", "TXN-MAX-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("20000.00"); // 2% of 999999.99
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("979999.99"); // 999999.99 - 20000.00
        }

        @Test
        @DisplayName("Should handle maximum reasonable transaction value for credit card")
        void shouldHandleMaximumReasonableTransactionValueForCreditCard() {
            // Arrange
            Money transactionValue = new Money("999999.99");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-MAX-2", "TXN-MAX-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("40000.00"); // 4% of 999999.99
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("959999.99"); // 999999.99 - 40000.00
        }

        @Test
        @DisplayName("Should handle transaction with rounded monetary value")
        void shouldHandleTransactionWithRoundedMonetaryValue() {
            // Arrange
            Money transactionValue = new Money("99.99");

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-ROUND-1", "TXN-ROUND-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-ROUND-2", "TXN-ROUND-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert
            assertThat(debitReceivable.getDiscount().getAmount()).isEqualByComparingTo("2.00"); // 2% of 99.99 = 1.9998 → 2.00
            assertThat(debitReceivable.getTotal().getAmount()).isEqualByComparingTo("97.99"); // 99.99 - 2.00

            assertThat(creditReceivable.getDiscount().getAmount()).isEqualByComparingTo("4.00"); // 4% of 99.99 = 3.9996 → 4.00
            assertThat(creditReceivable.getTotal().getAmount()).isEqualByComparingTo("95.99"); // 99.99 - 4.00
        }

        @Test
        @DisplayName("Should handle payment date calculation near day boundary for credit card")
        void shouldHandlePaymentDateCalculationNearDayBoundary() {
            // Arrange
            Money transactionValue = new Money("100.00");
            LocalDateTime expectedPaymentDate = LocalDateTime.now().plusDays(30);

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-BOUNDARY", "TXN-BOUNDARY", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Should be exactly 30 days from now (within 5 seconds tolerance)
            assertThat(receivable.getPaymentDate())
                    .isCloseTo(expectedPaymentDate, within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Should handle very long receivable and transaction IDs")
        void shouldHandleVeryLongIds() {
            // Arrange
            String longReceivableId = "RCV-" + "X".repeat(100);
            String longTransactionId = "TXN-" + "Y".repeat(100);
            Money transactionValue = new Money("50.00");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    longReceivableId, longTransactionId, transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert
            assertThat(receivable.getId()).isEqualTo(longReceivableId);
            assertThat(receivable.getTransactionId()).isEqualTo(longTransactionId);
            assertThat(receivable.getId()).hasSize(104); // "RCV-" + 100 X's
            assertThat(receivable.getTransactionId()).hasSize(104); // "TXN-" + 100 Y's
        }
    }

    @Nested
    @DisplayName("Calculation Accuracy Scenarios")
    class CalculationAccuracyScenarios {

        @Test
        @DisplayName("Should maintain precision in discount calculation for debit card")
        void shouldMaintainPrecisionInDiscountCalculationForDebitCard() {
            // Arrange
            Money transactionValue = new Money("123.67");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-PREC-1", "TXN-PREC-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            // Assert - 2% of 123.67 = 2.4734 → 2.47 (HALF_UP rounding)
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("2.47");
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("121.20"); // 123.67 - 2.47
        }

        @Test
        @DisplayName("Should maintain precision in discount calculation for credit card")
        void shouldMaintainPrecisionInDiscountCalculationForCreditCard() {
            // Arrange
            Money transactionValue = new Money("123.67");

            // Act
            Receivable receivable = receivableDomainService.calculateReceivable(
                    "RCV-PREC-2", "TXN-PREC-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - 4% of 123.67 = 4.9468 → 4.95 (HALF_UP rounding)
            assertThat(receivable.getDiscount().getAmount()).isEqualByComparingTo("4.95");
            assertThat(receivable.getTotal().getAmount()).isEqualByComparingTo("118.72"); // 123.67 - 4.95
        }

        @Test
        @DisplayName("Should ensure total equals transaction value minus discount")
        void shouldEnsureTotalEqualsTransactionValueMinusDiscount() {
            // Arrange
            Money transactionValue = new Money("456.78");

            // Act
            Receivable debitReceivable = receivableDomainService.calculateReceivable(
                    "RCV-MATH-1", "TXN-MATH-1", transactionValue, PaymentMethod.DEBIT_CARD
            );

            Receivable creditReceivable = receivableDomainService.calculateReceivable(
                    "RCV-MATH-2", "TXN-MATH-2", transactionValue, PaymentMethod.CREDIT_CARD
            );

            // Assert - Verify mathematical correctness
            BigDecimal debitCalculatedTotal = transactionValue.getAmount()
                    .subtract(debitReceivable.getDiscount().getAmount());
            assertThat(debitReceivable.getTotal().getAmount()).isEqualByComparingTo(debitCalculatedTotal);

            BigDecimal creditCalculatedTotal = transactionValue.getAmount()
                    .subtract(creditReceivable.getDiscount().getAmount());
            assertThat(creditReceivable.getTotal().getAmount()).isEqualByComparingTo(creditCalculatedTotal);
        }
    }
}
