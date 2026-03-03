package com.savadanko.aviasales.flight.controller;

import com.savadanko.aviasales.flight.dto.FlightOfferResponseList;
import com.savadanko.aviasales.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/flights")
    public ResponseEntity<FlightOfferResponseList> searchAndFilterFlights(
            // search-параметры
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) Integer passengers,

            // filter-параметры
            @RequestParam(required = false) Integer maxStops,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean baggage,
            @RequestParam(required = false) String airlines,
            @RequestParam(required = false) Integer maxDuration,

            // пагинация + сортировка
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "price.total") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        FlightOfferResponseList result = flightService.searchAndFilterFlights(
                origin,
                destination,
                departureDate,
                passengers,
                maxStops,
                maxPrice,
                baggage,
                airlines,
                maxDuration,
                pageNumber,
                pageSize,
                sortBy,
                sortDir
        );

        return ResponseEntity.ok(result);
    }
}
