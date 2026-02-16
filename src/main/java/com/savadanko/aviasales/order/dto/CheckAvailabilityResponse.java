package com.savadanko.aviasales.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckAvailabilityResponse {
    private CheckAvailabilityStatus status;
    private boolean isPriceChanged;
    private MoneyDto oldPrice;
    private MoneyDto finalPrice;
    private BigDecimal difference;
    private String message;
}
