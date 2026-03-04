package com.savadanko.aviasales.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchAirlineStatResponse {
    private String airline;
    private Long searches;
}
