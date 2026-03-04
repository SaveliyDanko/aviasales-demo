package com.savadanko.aviasales.flight.service;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.dto.FlightOfferResponse;
import com.savadanko.aviasales.flight.dto.FlightOfferResponseList;
import com.savadanko.aviasales.flight.dto.FlightSearchAirlineStatResponse;
import com.savadanko.aviasales.flight.dto.FlightSearchAnalyticsSummaryResponse;
import com.savadanko.aviasales.flight.dto.FlightSearchRouteStatResponse;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightOfferRepository flightOfferRepository;
    private final FlightOfferMapper mapper;
    private final FlightSearchMongoService flightSearchMongoService;

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

        SearchPageOptions pageOptions = createPageOptions(pageNumber, pageSize, sortBy, sortDir);
        FlightSearchCriteria criteria = new FlightSearchCriteria(
                origin,
                destination,
                departureDate,
                passengers,
                maxStops,
                maxPrice,
                baggage,
                airline,
                maxDuration,
                pageOptions.page(),
                pageOptions.size(),
                pageOptions.sortField(),
                pageOptions.sortDir()
        );

        String cacheKey = flightSearchMongoService.buildCacheKey(criteria);
        long startedAtMillis = System.currentTimeMillis();

        Optional<FlightOfferResponseList> cachedResult = flightSearchMongoService.findCachedResult(cacheKey);
        if (cachedResult.isPresent()) {
            FlightOfferResponseList response = cachedResult.get();
            long elapsed = System.currentTimeMillis() - startedAtMillis;
            flightSearchMongoService.recordSearchEvent(criteria, cacheKey, sizeOf(response), true, elapsed);
            return response;
        }

        Pageable pageable = createPageable(pageOptions);

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

        flightSearchMongoService.cacheResult(cacheKey, criteria, response);
        long elapsed = System.currentTimeMillis() - startedAtMillis;
        flightSearchMongoService.recordSearchEvent(criteria, cacheKey, sizeOf(response), false, elapsed);

        return response;
    }

    @Transactional(readOnly = true)
    public FlightSearchAnalyticsSummaryResponse getSearchAnalyticsSummary(int days) {
        return flightSearchMongoService.getSummary(days);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchRouteStatResponse> getTopRoutes(int days, int limit) {
        return flightSearchMongoService.getTopRoutes(days, limit);
    }

    @Transactional(readOnly = true)
    public List<FlightSearchAirlineStatResponse> getTopAirlines(int days, int limit) {
        return flightSearchMongoService.getTopAirlines(days, limit);
    }

    private Pageable createPageable(SearchPageOptions pageOptions) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(pageOptions.sortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(pageOptions.page(), pageOptions.size(), Sort.by(direction, pageOptions.sortField()));
    }

    private SearchPageOptions createPageOptions(int page, int size, String sortBy, String sortDir) {
        int validPage = Math.max(0, page);
        int validSize = (size > 0 && size <= 100) ? size : 20;
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "price.total";
        String validSortDir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        return new SearchPageOptions(validPage, validSize, sortField, validSortDir);
    }

    private int sizeOf(FlightOfferResponseList response) {
        return response.getContent() == null ? 0 : response.getContent().size();
    }

    private record SearchPageOptions(
            int page,
            int size,
            String sortField,
            String sortDir
    ) {
    }
}
