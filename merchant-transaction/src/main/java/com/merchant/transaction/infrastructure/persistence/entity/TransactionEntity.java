package com.merchant.transaction.infrastructure.persistence.entity;

import com.merchant.transaction.domain.model.PaymentMethod;
import com.merchant.transaction.domain.model.TransactionStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transactions")
public class TransactionEntity implements Persistable<String> {

    @Id
    private String id;

    @Column("value")
    private BigDecimal value;

    @Column("description")
    private String description;

    @Column("payment_method")
    private PaymentMethod method;

    @Column("card_number")
    private String cardNumber;

    @Column("merchant_name")
    private String merchantName;

    @Column("customer_name")
    private String customerName;

    @Column("status")
    private TransactionStatus status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
}



