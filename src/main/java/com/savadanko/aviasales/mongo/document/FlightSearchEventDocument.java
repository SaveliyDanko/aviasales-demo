package com.savadanko.aviasales.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "flight_search_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchEventDocument {

    @Id
    private String id;

    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant searchedAt;

    @Indexed
    private String cacheKey;

    private String origin;
    private String destination;
    private String route;
    private LocalDate departureDate;
    private Integer passengers;
    private Integer maxStops;
    private BigDecimal maxPrice;
    private Boolean baggage;
    private String airline;
    private Integer maxDuration;
    private Integer pageNumber;
    private Integer pageSize;
    private String sortBy;
    private String sortDir;

    private Integer resultCount;
    private Long responseTimeMs;
    private Boolean cacheHit;
}
