package com.savadanko.aviasales.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaggageRequest {
    @NotNull
    private Integer passengerId;

    @NotNull
    @Min(value = 0, message = "count must be >= 0")
    private Integer count;

    private Integer weight;
}

