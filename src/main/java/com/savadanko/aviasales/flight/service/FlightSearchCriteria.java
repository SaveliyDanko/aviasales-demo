package com.savadanko.aviasales.flight.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FlightSearchCriteria(
        String origin,
        String destination,
        LocalDate departureDate,
        Integer passengers,
        Integer maxStops,
        BigDecimal maxPrice,
        Boolean baggage,
        String airline,
        Integer maxDuration,
        Integer pageNumber,
        Integer pageSize,
        String sortBy,
        String sortDir
) {
}
