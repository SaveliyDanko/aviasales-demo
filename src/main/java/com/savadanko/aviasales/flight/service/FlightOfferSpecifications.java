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
import java.util.Locale;

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
            predicates.add(cb.greaterThan(root.get("passengers").get("countBookable"), 0));

            if (passengers != null && passengers > 0) {
                Expression<Integer> countBookable = root.get("passengers").get("countBookable");
                predicates.add(cb.greaterThanOrEqualTo(countBookable, passengers));
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
            query.distinct(true);

            if (departureDate != null) {
                ZoneOffset offset = ZoneOffset.ofHours(3);
                OffsetDateTime start = departureDate.atStartOfDay().atOffset(offset);
                OffsetDateTime nextDay = departureDate.plusDays(1).atStartOfDay().atOffset(offset);

                predicates.add(cb.greaterThanOrEqualTo(segment.get("departure").get("at"), start));
                predicates.add(cb.lessThan(segment.get("departure").get("at"), nextDay));
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
                String a = airline.trim().toLowerCase(Locale.ROOT);
                predicates.add(cb.equal(cb.lower(segment.get("carrier").get("operatingName")), a));
            }

            // Фильтр по багажу
            if (Boolean.TRUE.equals(baggage)) {
                predicates.add(cb.greaterThan(segment.get("baggage").get("checked").get("allowance"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
