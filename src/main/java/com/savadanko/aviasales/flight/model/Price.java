package com.savadanko.aviasales.flight.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Price {
    private String currency;
    private BigDecimal total;
    private BigDecimal base;
    private BigDecimal taxes;
}
