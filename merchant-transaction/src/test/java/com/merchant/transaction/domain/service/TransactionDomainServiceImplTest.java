package com.merchant.transaction.domain.service;

import com.merchant.transaction.domain.exception.InvalidTransactionException;
import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.Transaction;
import com.merchant.transaction.domain.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransactionDomainServiceImpl Unit Tests")
class TransactionDomainServiceImplTest {

    private TransactionDomainServiceImpl transactionDomainService;

    @BeforeEach
    void setUp() {
        transactionDomainService = new TransactionDomainServiceImpl();
    }

    @Nested
    @DisplayName("Successful Transaction Creation Scenarios")
    class SuccessfulTransactionCreation {

        @Test
        @DisplayName("Should create transaction with valid debit card payment")
        void shouldCreateTransactionWithValidDebitCard() {
            // Arrange
            String id = "TXN-001";
            String value = "100.50";
            String description = "Purchase at Store ABC";
            PaymentMethod method = PaymentMethod.DEBIT_CARD;
            String cardNumber = "1234567890123456";
            String merchantName = "Store ABC";
            String customerName = "John Doe";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    id, value, description, method, cardNumber, merchantName, customerName
            );

            // Assert
            assertThat(transaction).isNotNull();
            assertThat(transaction.getId()).isEqualTo(id);
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("100.50");
            assertThat(transaction.getDescription()).isEqualTo(description);
            assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
            assertThat(transaction.getCardNumber()).isEqualTo("3456"); // Last 4 digits
            assertThat(transaction.getMerchantName()).isEqualTo(merchantName);
            assertThat(transaction.getCustomerName()).isEqualTo(customerName);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(transaction.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create transaction with valid credit card payment")
        void shouldCreateTransactionWithValidCreditCard() {
            // Arrange
            String id = "TXN-002";
            String value = "250.75";
            String description = "Online purchase";
            PaymentMethod method = PaymentMethod.CREDIT_CARD;
            String cardNumber = "9876543210987654";
            String merchantName = "Online Store XYZ";
            String customerName = "Jane Smith";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    id, value, description, method, cardNumber, merchantName, customerName
            );

            // Assert
            assertThat(transaction).isNotNull();
            assertThat(transaction.getId()).isEqualTo(id);
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("250.75");
            assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(transaction.getCardNumber()).isEqualTo("7654"); // Last 4 digits
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("Should create transaction with minimum valid amount")
        void shouldCreateTransactionWithMinimumValidAmount() {
            // Arrange
            String value = "0.01"; // Minimum positive value

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-003", value, "Small transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant A", "Customer A"
            );

            // Assert
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("0.01");
        }

        @Test
        @DisplayName("Should create transaction with large amount")
        void shouldCreateTransactionWithLargeAmount() {
            // Arrange
            String value = "999999.99";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-004", value, "Large transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant B", "Customer B"
            );

            // Assert
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("999999.99");
        }

        @Test
        @DisplayName("Should create transaction with exactly 4 digit card number")
        void shouldCreateTransactionWithMinimumCardNumberLength() {
            // Arrange
            String cardNumber = "1234"; // Exactly 4 digits

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-005", "50.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant C", "Customer C"
            );

            // Assert
            assertThat(transaction.getCardNumber()).isEqualTo("1234");
        }

        @Test
        @DisplayName("Should create transaction with decimal value without trailing zeros")
        void shouldCreateTransactionWithDecimalValue() {
            // Arrange
            String value = "100";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-006", value, "Round amount", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant D", "Customer D"
            );

            // Assert
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("Should create transaction with multiple decimal places and round to 2 decimals")
        void shouldCreateTransactionWithMultipleDecimalPlaces() {
            // Arrange
            String value = "123.456789";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-007", value, "Precise amount", PaymentMethod.CREDIT_CARD,
                    "9876543210", "Merchant E", "Customer E"
            );

            // Assert - Money class rounds to 2 decimal places using HALF_UP
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("123.46");
        }
    }

    @Nested
    @DisplayName("Transaction Value Validation Scenarios")
    class TransactionValueValidation {

        @Test
        @DisplayName("Should throw exception when transaction value is zero")
        void shouldThrowExceptionWhenValueIsZero() {
            // Arrange
            String value = "0";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-008", value, "Invalid transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant F", "Customer F"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Transaction value must be positive");
        }

        @Test
        @DisplayName("Should throw exception when transaction value is negative")
        void shouldThrowExceptionWhenValueIsNegative() {
            // Arrange
            String value = "-50.00";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-009", value, "Invalid transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant G", "Customer G"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Transaction value must be positive");
        }

        @Test
        @DisplayName("Should throw exception when transaction value is invalid string")
        void shouldThrowExceptionWhenValueIsInvalidString() {
            // Arrange
            String value = "invalid-amount";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-010", value, "Invalid transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant H", "Customer H"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Invalid transaction value format");
        }

        @Test
        @DisplayName("Should throw exception when transaction value is empty string")
        void shouldThrowExceptionWhenValueIsEmptyString() {
            // Arrange
            String value = "";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-011", value, "Invalid transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant I", "Customer I"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Invalid transaction value format");
        }

        @Test
        @DisplayName("Should throw exception when transaction value contains non-numeric characters")
        void shouldThrowExceptionWhenValueContainsNonNumericCharacters() {
            // Arrange
            String value = "100.50$";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-012", value, "Invalid transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant J", "Customer J"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Invalid transaction value format");
        }

        @Test
        @DisplayName("Should throw exception when transaction value is negative zero")
        void shouldThrowExceptionWhenValueIsNegativeZero() {
            // Arrange
            String value = "-0.00";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-013", value, "Invalid transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant K", "Customer K"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Transaction value must be positive");
        }
    }

    @Nested
    @DisplayName("Card Number Validation Scenarios")
    class CardNumberValidation {

        @Test
        @DisplayName("Should throw exception when card number is null")
        void shouldThrowExceptionWhenCardNumberIsNull() {
            // Arrange
            String cardNumber = null;

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-014", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant L", "Customer L"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }

        @Test
        @DisplayName("Should throw exception when card number has less than 4 digits")
        void shouldThrowExceptionWhenCardNumberHasLessThan4Digits() {
            // Arrange
            String cardNumber = "123"; // Only 3 digits

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-015", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    cardNumber, "Merchant M", "Customer M"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }

        @Test
        @DisplayName("Should throw exception when card number is empty string")
        void shouldThrowExceptionWhenCardNumberIsEmptyString() {
            // Arrange
            String cardNumber = "";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-016", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant N", "Customer N"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }

        @Test
        @DisplayName("Should throw exception when card number has exactly 1 digit")
        void shouldThrowExceptionWhenCardNumberHasOneDigit() {
            // Arrange
            String cardNumber = "1";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-017", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    cardNumber, "Merchant O", "Customer O"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }

        @Test
        @DisplayName("Should throw exception when card number has exactly 2 digits")
        void shouldThrowExceptionWhenCardNumberHasTwoDigits() {
            // Arrange
            String cardNumber = "12";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-018", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant P", "Customer P"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }
    }

    @Nested
    @DisplayName("Merchant Name Validation Scenarios")
    class MerchantNameValidation {

        @Test
        @DisplayName("Should throw exception when merchant name is null")
        void shouldThrowExceptionWhenMerchantNameIsNull() {
            // Arrange
            String merchantName = null;

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-019", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer Q"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }

        @Test
        @DisplayName("Should throw exception when merchant name is empty string")
        void shouldThrowExceptionWhenMerchantNameIsEmptyString() {
            // Arrange
            String merchantName = "";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-020", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", merchantName, "Customer R"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }

        @Test
        @DisplayName("Should throw exception when merchant name is blank (spaces only)")
        void shouldThrowExceptionWhenMerchantNameIsBlank() {
            // Arrange
            String merchantName = "   "; // Only spaces

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-021", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer S"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }

        @Test
        @DisplayName("Should throw exception when merchant name contains only tabs")
        void shouldThrowExceptionWhenMerchantNameContainsOnlyTabs() {
            // Arrange
            String merchantName = "\t\t\t";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-022", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", merchantName, "Customer T"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }

        @Test
        @DisplayName("Should throw exception when merchant name contains only newlines")
        void shouldThrowExceptionWhenMerchantNameContainsOnlyNewlines() {
            // Arrange
            String merchantName = "\n\n";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-023", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer U"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }
    }

    @Nested
    @DisplayName("Customer Name Validation Scenarios")
    class CustomerNameValidation {

        @Test
        @DisplayName("Should throw exception when customer name is null")
        void shouldThrowExceptionWhenCustomerNameIsNull() {
            // Arrange
            String customerName = null;

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-024", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant V", customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }

        @Test
        @DisplayName("Should throw exception when customer name is empty string")
        void shouldThrowExceptionWhenCustomerNameIsEmptyString() {
            // Arrange
            String customerName = "";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-025", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant W", customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }

        @Test
        @DisplayName("Should throw exception when customer name is blank (spaces only)")
        void shouldThrowExceptionWhenCustomerNameIsBlank() {
            // Arrange
            String customerName = "   "; // Only spaces

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-026", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant X", customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }

        @Test
        @DisplayName("Should throw exception when customer name contains only tabs")
        void shouldThrowExceptionWhenCustomerNameContainsOnlyTabs() {
            // Arrange
            String customerName = "\t\t\t";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-027", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant Y", customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }

        @Test
        @DisplayName("Should throw exception when customer name contains only newlines")
        void shouldThrowExceptionWhenCustomerNameContainsOnlyNewlines() {
            // Arrange
            String customerName = "\n\n";

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-028", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", "Merchant Z", customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should create transaction with single character merchant name")
        void shouldCreateTransactionWithSingleCharacterMerchantName() {
            // Arrange
            String merchantName = "A";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-029", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer AA"
            );

            // Assert
            assertThat(transaction.getMerchantName()).isEqualTo("A");
        }

        @Test
        @DisplayName("Should create transaction with single character customer name")
        void shouldCreateTransactionWithSingleCharacterCustomerName() {
            // Arrange
            String customerName = "B";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-030", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant AB", customerName
            );

            // Assert
            assertThat(transaction.getCustomerName()).isEqualTo("B");
        }

        @Test
        @DisplayName("Should create transaction with very long merchant name")
        void shouldCreateTransactionWithVeryLongMerchantName() {
            // Arrange
            String merchantName = "A".repeat(1000);

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-031", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer AC"
            );

            // Assert
            assertThat(transaction.getMerchantName()).hasSize(1000);
        }

        @Test
        @DisplayName("Should create transaction with very long customer name")
        void shouldCreateTransactionWithVeryLongCustomerName() {
            // Arrange
            String customerName = "B".repeat(1000);

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-032", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant AD", customerName
            );

            // Assert
            assertThat(transaction.getCustomerName()).hasSize(1000);
        }

        @Test
        @DisplayName("Should create transaction with merchant name containing special characters")
        void shouldCreateTransactionWithMerchantNameContainingSpecialCharacters() {
            // Arrange
            String merchantName = "Store & Co. #123!";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-033", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer AE"
            );

            // Assert
            assertThat(transaction.getMerchantName()).isEqualTo("Store & Co. #123!");
        }

        @Test
        @DisplayName("Should create transaction with customer name containing special characters")
        void shouldCreateTransactionWithCustomerNameContainingSpecialCharacters() {
            // Arrange
            String customerName = "O'Brien-Smith Jr.";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-034", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant AF", customerName
            );

            // Assert
            assertThat(transaction.getCustomerName()).isEqualTo("O'Brien-Smith Jr.");
        }

        @Test
        @DisplayName("Should create transaction with merchant name containing leading/trailing spaces")
        void shouldCreateTransactionWithMerchantNameContainingLeadingTrailingSpaces() {
            // Arrange
            String merchantName = "  Valid Merchant  ";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-035", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    "1234567890", merchantName, "Customer AG"
            );

            // Assert - isBlank() only checks if all whitespace, so this is valid
            assertThat(transaction.getMerchantName()).isEqualTo("  Valid Merchant  ");
        }

        @Test
        @DisplayName("Should create transaction with customer name containing leading/trailing spaces")
        void shouldCreateTransactionWithCustomerNameContainingLeadingTrailingSpaces() {
            // Arrange
            String customerName = "  Valid Customer  ";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-036", "100.00", "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant AH", customerName
            );

            // Assert
            assertThat(transaction.getCustomerName()).isEqualTo("  Valid Customer  ");
        }

        @Test
        @DisplayName("Should create transaction with very long card number")
        void shouldCreateTransactionWithVeryLongCardNumber() {
            // Arrange
            String cardNumber = "1234567890123456789012345678901234567890";

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-037", "100.00", "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant AI", "Customer AI"
            );

            // Assert - Should extract last 4 digits
            assertThat(transaction.getCardNumber()).isEqualTo("7890");
        }

        @Test
        @DisplayName("Should create transaction with scientific notation value")
        void shouldCreateTransactionWithScientificNotationValue() {
            // Arrange
            String value = "1.5E+2"; // 150.0 in scientific notation

            // Act
            Transaction transaction = transactionDomainService.createTransaction(
                    "TXN-038", value, "Test transaction", PaymentMethod.CREDIT_CARD,
                    "1234567890", "Merchant AJ", "Customer AJ"
            );

            // Assert
            assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("150");
        }
    }

    @Nested
    @DisplayName("Multiple Validation Failures")
    class MultipleValidationFailures {

        @Test
        @DisplayName("Should fail on first validation error when value and card number are invalid")
        void shouldFailOnFirstValidationErrorWhenValueAndCardNumberInvalid() {
            // Arrange - Value validation comes first, should fail before card validation
            String value = "invalid";
            String cardNumber = "12"; // Less than 4 digits

            // Act & Assert - Should fail on value validation
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-039", value, "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, "Merchant AK", "Customer AK"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Invalid transaction value format");
        }

        @Test
        @DisplayName("Should fail on second validation when value is valid but card number is invalid")
        void shouldFailOnSecondValidationWhenValueValidButCardNumberInvalid() {
            // Arrange
            String value = "100.00"; // Valid
            String cardNumber = "12"; // Invalid

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-040", value, "Test transaction", PaymentMethod.CREDIT_CARD,
                    cardNumber, "Merchant AL", "Customer AL"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Card number must have at least 4 digits");
        }

        @Test
        @DisplayName("Should fail on merchant name validation when card is valid")
        void shouldFailOnMerchantNameValidationWhenCardIsValid() {
            // Arrange
            String value = "100.00"; // Valid
            String cardNumber = "1234567890"; // Valid
            String merchantName = ""; // Invalid

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-041", value, "Test transaction", PaymentMethod.DEBIT_CARD,
                    cardNumber, merchantName, "Customer AM"
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Merchant name is required");
        }

        @Test
        @DisplayName("Should fail on customer name validation when all other fields are valid")
        void shouldFailOnCustomerNameValidationWhenAllOtherFieldsValid() {
            // Arrange
            String value = "100.00"; // Valid
            String cardNumber = "1234567890"; // Valid
            String merchantName = "Valid Merchant"; // Valid
            String customerName = ""; // Invalid

            // Act & Assert
            assertThatThrownBy(() -> transactionDomainService.createTransaction(
                    "TXN-042", value, "Test transaction", PaymentMethod.CREDIT_CARD,
                    cardNumber, merchantName, customerName
            ))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessage("Customer name is required");
        }
    }
}
