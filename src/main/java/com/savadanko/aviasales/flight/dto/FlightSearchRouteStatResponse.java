package com.savadanko.aviasales.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRouteStatResponse {
    private String origin;
    private String destination;
    private Long searches;
}
