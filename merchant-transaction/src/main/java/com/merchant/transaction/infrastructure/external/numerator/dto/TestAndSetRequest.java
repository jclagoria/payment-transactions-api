package com.merchant.transaction.infrastructure.external.numerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAndSetRequest {

    private Long oldValue;
    private Long newValue;

}
