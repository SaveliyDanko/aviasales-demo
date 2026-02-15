package com.savadanko.aviasales.booking.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AncillariesRequest {
    @Valid
    private List<BaggageRequest> baggage = new ArrayList<>();

    @Valid
    private InsuranceRequest insurance;
}

