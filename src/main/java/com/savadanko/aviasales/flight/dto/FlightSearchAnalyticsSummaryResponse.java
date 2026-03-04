package com.savadanko.aviasales.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchAnalyticsSummaryResponse {
    private Integer days;
    private Long totalSearches;
    private Long cacheHits;
    private Double cacheHitRate;
}
