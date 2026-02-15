package com.savadanko.aviasales.flight.service;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.Itinerary;
import com.savadanko.aviasales.flight.Segment;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class FlightOfferSpecifications {
    public static Specification<FlightOffer> buildSpec(
            String origin,
            String destination,
            LocalDate departureDate,
            Integer passengers,
            Integer maxStops,
            BigDecimal maxPrice,
            Boolean baggage,
            String airline,
            Integer maxDuration
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isBookable")));

            // Фильтр по пассажирам
            if (passengers != null && passengers > 0) {
                Expression<Integer> adults = root.get("passengers").get("adults");
                Expression<Integer> children = root.get("passengers").get("children");
                predicates.add(cb.greaterThanOrEqualTo(cb.sum(adults, children), passengers));
            }

            // Фильтр по максимальной цене
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price").get("total"), maxPrice));
            }

            // join itineraries
            Join<FlightOffer, Itinerary> itinerary = root.join("itineraries", JoinType.INNER);

            predicates.add(cb.equal(itinerary.get("direction"), "OUTBOUND"));

            // Фильтр по длительности
            if (maxDuration != null) {
                predicates.add(cb.lessThanOrEqualTo(itinerary.get("durationMinutes"), maxDuration));
            }

            // Фильтр по пересадкам
            if (maxStops != null) {
                predicates.add(cb.lessThanOrEqualTo(itinerary.get("stops"), maxStops));
            }

            // join segment
            Join<Itinerary, Segment> segment = itinerary.join("segments", JoinType.INNER);

            // Фильтр по дате вылета (в пределах дня)
            if (departureDate != null) {
                ZoneOffset offset = ZoneOffset.ofHours(3);
                OffsetDateTime startOfDay = departureDate.atStartOfDay().atOffset(offset);
                OffsetDateTime endOfDay = departureDate.atTime(23, 59, 59).atOffset(offset);

                predicates.add(cb.between(segment.get("departure").get("at"), startOfDay, endOfDay));
            }

            // Фильтр по origin/destination
            if (StringUtils.hasText(origin)) {
                predicates.add(cb.equal(segment.get("departure").get("iataCode"), origin));
            }
            if (StringUtils.hasText(destination)) {
                predicates.add(cb.equal(segment.get("arrival").get("iataCode"), destination));
            }

            // Фильтр по авиакомпании
            if (StringUtils.hasText(airline)) {
                predicates.add(cb.equal(segment.get("carrier").get("operatingName"), airline));
            }

            // Фильтр по багажу
            if (Boolean.TRUE.equals(baggage)) {
                predicates.add(cb.greaterThan(segment.get("baggage").get("checked").get("allowance"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
