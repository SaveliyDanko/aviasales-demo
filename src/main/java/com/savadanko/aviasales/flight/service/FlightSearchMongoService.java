package com.savadanko.aviasales.flight.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.savadanko.aviasales.flight.dto.FlightOfferResponseList;
import com.savadanko.aviasales.flight.dto.FlightSearchAirlineStatResponse;
import com.savadanko.aviasales.flight.dto.FlightSearchAnalyticsSummaryResponse;
import com.savadanko.aviasales.flight.dto.FlightSearchRouteStatResponse;
import com.savadanko.aviasales.mongo.document.FlightSearchCacheDocument;
import com.savadanko.aviasales.mongo.document.FlightSearchEventDocument;
import com.savadanko.aviasales.mongo.projection.TopAirlineProjection;
import com.savadanko.aviasales.mongo.projection.TopRouteProjection;
import com.savadanko.aviasales.mongo.repository.FlightSearchCacheRepository;
import com.savadanko.aviasales.mongo.repository.FlightSearchEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSearchMongoService {

    private final FlightSearchCacheRepository cacheRepository;
    private final FlightSearchEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.mongo.flight-cache-ttl-minutes:15}")
    private long flightCacheTtlMinutes;

    public String buildCacheKey(FlightSearchCriteria criteria) {
        String canonical = String.join(
                "|",
                safe(normalizeCode(criteria.origin())),
                safe(normalizeCode(criteria.destination())),
                safe(criteria.departureDate()),
                safe(criteria.passengers()),
                safe(criteria.maxStops()),
                safe(toMoney(criteria.maxPrice())),
                safe(criteria.baggage()),
                safe(normalizeText(criteria.airline())),
                safe(criteria.maxDuration()),
                safe(criteria.pageNumber()),
                safe(criteria.pageSize()),
                safe(criteria.sortBy()),
                safe(normalizeSortDir(criteria.sortDir()))
        );

        return DigestUtils.md5DigestAsHex(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<FlightOfferResponseList> findCachedResult(String cacheKey) {
        try {
            Optional<FlightSearchCacheDocument> cached = cacheRepository.findByCacheKey(cacheKey);
            if (cached.isEmpty()) {
                return Optional.empty();
            }

            FlightSearchCacheDocument cachedDocument = cached.get();
            cachedDocument.setLastAccessedAt(Instant.now());
            Long currentHits = cachedDocument.getHitCount() == null ? 0L : cachedDocument.getHitCount();
            cachedDocument.setHitCount(currentHits + 1);
            cacheRepository.save(cachedDocument);

            FlightOfferResponseList response = objectMapper.readValue(
                    cachedDocument.getPayloadJson(),
                    FlightOfferResponseList.class
            );
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Mongo cache read failed, fallback to SQL search. cacheKey={}", cacheKey, e);
            return Optional.empty();
        }
    }

    public void cacheResult(String cacheKey, FlightSearchCriteria criteria, FlightOfferResponseList response) {
        try {
            Instant now = Instant.now();
            FlightSearchCacheDocument document = cacheRepository.findByCacheKey(cacheKey)
                    .orElseGet(FlightSearchCacheDocument::new);

            document.setCacheKey(cacheKey);
            document.setOrigin(normalizeCode(criteria.origin()));
            document.setDestination(normalizeCode(criteria.destination()));
            document.setDepartureDate(criteria.departureDate());
            document.setPassengers(criteria.passengers());
            document.setMaxStops(criteria.maxStops());
            document.setMaxPrice(criteria.maxPrice());
            document.setBaggage(criteria.baggage());
            document.setAirline(normalizeText(criteria.airline()));
            document.setMaxDuration(criteria.maxDuration());
            document.setPageNumber(criteria.pageNumber());
            document.setPageSize(criteria.pageSize());
            document.setSortBy(criteria.sortBy());
            document.setSortDir(normalizeSortDir(criteria.sortDir()));
            document.setPayloadJson(toJson(response));
            document.setOfferIds(extractOfferIds(response));
            document.setResultSize(response.getContent() == null ? 0 : response.getContent().size());
            document.setTotalElements(response.getTotalElements());
            document.setTotalPages(response.getTotalPages());
            document.setCreatedAt(now);
            document.setLastAccessedAt(now);
            document.setHitCount(0L);
            document.setExpireAt(now.plus(Duration.ofMinutes(normalizeCacheTtlMinutes())));

            cacheRepository.save(document);
        } catch (Exception e) {
            log.warn("Mongo cache write failed. cacheKey={}", cacheKey, e);
        }
    }

    public void recordSearchEvent(
            FlightSearchCriteria criteria,
            String cacheKey,
            int resultCount,
            boolean cacheHit,
            long responseTimeMs
    ) {
        try {
            FlightSearchEventDocument document = FlightSearchEventDocument.builder()
                    .searchedAt(Instant.now())
                    .cacheKey(cacheKey)
                    .origin(normalizeCode(criteria.origin()))
                    .destination(normalizeCode(criteria.destination()))
                    .route(buildRoute(criteria.origin(), criteria.destination()))
                    .departureDate(criteria.departureDate())
                    .passengers(criteria.passengers())
                    .maxStops(criteria.maxStops())
                    .maxPrice(criteria.maxPrice())
                    .baggage(criteria.baggage())
                    .airline(normalizeText(criteria.airline()))
                    .maxDuration(criteria.maxDuration())
                    .pageNumber(criteria.pageNumber())
                    .pageSize(criteria.pageSize())
                    .sortBy(criteria.sortBy())
                    .sortDir(normalizeSortDir(criteria.sortDir()))
                    .resultCount(Math.max(0, resultCount))
                    .responseTimeMs(Math.max(0L, responseTimeMs))
                    .cacheHit(cacheHit)
                    .build();

            eventRepository.save(document);
        } catch (Exception e) {
            log.warn("Mongo search event write failed. cacheKey={}", cacheKey, e);
        }
    }

    public FlightSearchAnalyticsSummaryResponse getSummary(int days) {
        int normalizedDays = normalizeDays(days);
        Instant from = Instant.now().minus(Duration.ofDays(normalizedDays));

        try {
            long totalSearches = eventRepository.countBySearchedAtGreaterThanEqual(from);
            long cacheHits = eventRepository.countBySearchedAtGreaterThanEqualAndCacheHitTrue(from);
            double cacheHitRate = totalSearches == 0
                    ? 0.0
                    : BigDecimal.valueOf(cacheHits)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSearches), 2, RoundingMode.HALF_UP)
                    .doubleValue();

            return new FlightSearchAnalyticsSummaryResponse(
                    normalizedDays,
                    totalSearches,
                    cacheHits,
                    cacheHitRate
            );
        } catch (Exception e) {
            log.warn("Mongo summary aggregation failed, returning empty summary.", e);
            return new FlightSearchAnalyticsSummaryResponse(normalizedDays, 0L, 0L, 0.0);
        }
    }

    public List<FlightSearchRouteStatResponse> getTopRoutes(int days, int limit) {
        int normalizedDays = normalizeDays(days);
        int normalizedLimit = normalizeLimit(limit);
        Instant from = Instant.now().minus(Duration.ofDays(normalizedDays));

        try {
            List<TopRouteProjection> topRoutes = eventRepository.findTopRoutes(from, normalizedLimit);
            return topRoutes.stream()
                    .map(stat -> new FlightSearchRouteStatResponse(
                            stat.getOrigin(),
                            stat.getDestination(),
                            stat.getSearches()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Mongo route aggregation failed.", e);
            return Collections.emptyList();
        }
    }

    public List<FlightSearchAirlineStatResponse> getTopAirlines(int days, int limit) {
        int normalizedDays = normalizeDays(days);
        int normalizedLimit = normalizeLimit(limit);
        Instant from = Instant.now().minus(Duration.ofDays(normalizedDays));

        try {
            List<TopAirlineProjection> topAirlines = eventRepository.findTopAirlines(from, normalizedLimit);
            return topAirlines.stream()
                    .map(stat -> new FlightSearchAirlineStatResponse(
                            stat.getAirline(),
                            stat.getSearches()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("Mongo airline aggregation failed.", e);
            return Collections.emptyList();
        }
    }

    private String toJson(FlightOfferResponseList response) throws JsonProcessingException {
        return objectMapper.writeValueAsString(response);
    }

    private List<String> extractOfferIds(FlightOfferResponseList response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return Collections.emptyList();
        }
        return response.getContent().stream()
                .map(flightOffer -> flightOffer.getOfferId())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String buildRoute(String origin, String destination) {
        String normalizedOrigin = normalizeCode(origin);
        String normalizedDestination = normalizeCode(destination);
        if (normalizedOrigin == null || normalizedDestination == null) {
            return null;
        }
        return normalizedOrigin + "-" + normalizedDestination;
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSortDir(String sortDir) {
        if ("DESC".equalsIgnoreCase(sortDir)) {
            return "DESC";
        }
        return "ASC";
    }

    private String toMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private int normalizeDays(int days) {
        if (days <= 0) {
            return 30;
        }
        return Math.min(days, 365);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private long normalizeCacheTtlMinutes() {
        return flightCacheTtlMinutes > 0 ? flightCacheTtlMinutes : 15;
    }

    private String safe(Object value) {
        return value == null ? "_" : String.valueOf(value);
    }
}
