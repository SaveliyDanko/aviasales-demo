package com.savadanko.aviasales.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityRequest {
    @NotBlank
    private String offerId;

    @NotNull
    @Valid
    private MoneyDto expectedPrice;
}

