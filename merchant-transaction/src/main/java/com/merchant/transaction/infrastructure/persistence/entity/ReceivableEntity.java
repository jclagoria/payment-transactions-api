package com.merchant.transaction.infrastructure.persistence.entity;

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
@Table("receivables")
public class ReceivableEntity implements Persistable<String> {

    @Id
    private String id;

    @Column("transaction_id")
    private String transactionId;

    @Column("status")
    private String status;

    @Column("create_date")
    private LocalDateTime createDate;

    @Column("payment_date")
    private LocalDateTime paymentDate;

    @Column("subtotal")
    private BigDecimal subTotal;

    @Column("discount")
    private BigDecimal discount;

    @Column("total")
    private BigDecimal total;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
}
