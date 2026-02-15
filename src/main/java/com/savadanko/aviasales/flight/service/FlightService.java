package com.savadanko.aviasales.flight.service;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.dto.FlightOfferResponse;
import com.savadanko.aviasales.flight.dto.FlightOfferResponseList;
import com.savadanko.aviasales.flight.mapper.FlightOfferMapper;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightOfferRepository flightOfferRepository;
    private  final FlightOfferMapper mapper;

    @Transactional(readOnly = true)
    public FlightOfferResponseList searchAndFilterFlights(
            String origin,
            String destination,
            LocalDate departureDate,
            Integer passengers,
            Integer maxStops,
            BigDecimal maxPrice,
            Boolean baggage,
            String airline,
            Integer maxDuration,
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDir
    ) {
        log.info(
                "Search/filter flights: origin={}, destination={}, departureDate={}, passengers={}, maxStops={}, maxPrice={}, baggage={}, airline={}, maxDuration={}",
                origin, destination, departureDate, passengers, maxStops, maxPrice, baggage, airline, maxDuration
        );

        Pageable pageable = createPageable(pageNumber, pageSize, sortBy, sortDir);

        Specification<FlightOffer> spec = FlightOfferSpecifications.buildSpec(
                origin,
                destination,
                departureDate,
                passengers,
                maxStops,
                maxPrice,
                baggage,
                airline,
                maxDuration
        );

        Page<FlightOffer> flightOfferPage = flightOfferRepository.findAll(spec, pageable);

        List<FlightOfferResponse> flightOfferResponses = flightOfferPage.getContent().stream()
                .map(mapper::toDto)
                .toList();

        FlightOfferResponseList response = new FlightOfferResponseList();
        response.setContent(flightOfferResponses);
        response.setPageNumber(flightOfferPage.getNumber());
        response.setPageSize(flightOfferPage.getSize());
        response.setTotalElements(flightOfferPage.getTotalElements());
        response.setTotalPages(flightOfferPage.getTotalPages());
        response.setLastPage(flightOfferPage.isLast());

        return response;
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        // Защита от кривых параметров
        int validPage = Math.max(0, page);
        int validSize = (size > 0 && size <= 100) ? size : 20;

        // По умолчанию сортируем по цене
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "price.total";
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        return PageRequest.of(validPage, validSize, Sort.by(direction, sortField));
    }
}
