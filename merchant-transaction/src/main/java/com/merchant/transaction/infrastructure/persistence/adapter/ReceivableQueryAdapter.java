package com.merchant.transaction.infrastructure.persistence.adapter;

import com.merchant.transaction.application.query.dto.ReceivableProjection;
import com.merchant.transaction.application.query.port.ReceivableQueryPort;
import com.merchant.transaction.infrastructure.persistence.r2dbc.ReceivableR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReceivableQueryAdapter implements ReceivableQueryPort {

    private final ReceivableR2dbcRepository repository;

    @Override
    public Flux<ReceivableProjection> finByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        return repository.findByCreateDateBetween(startDate, endDate)
                .map(entity -> ReceivableProjection.builder()
                        .subtotal(entity.getSubTotal())
                        .discount(entity.getDiscount())
                        .total(entity.getTotal())
                        .status(entity.getStatus())
                        .createDate(entity.getCreateDate())
                        .build());
    }
}
