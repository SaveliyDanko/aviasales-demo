package com.savadanko.aviasales.flight;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.savadanko.aviasales.flight.model.Aircraft;
import com.savadanko.aviasales.flight.model.Baggage;
import com.savadanko.aviasales.flight.model.Carrier;
import com.savadanko.aviasales.flight.model.Location;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "segments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Segment {
    @Id
    @Column(name = "segment_id", updatable = false, nullable = false)
    private String segmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    @JsonIgnore
    private Itinerary itinerary;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "iataCode", column = @Column(name = "dep_iata_code")),
            @AttributeOverride(name = "city", column = @Column(name = "dep_city")),
            @AttributeOverride(name = "terminal", column = @Column(name = "dep_terminal")),
            @AttributeOverride(name = "at", column = @Column(name = "dep_at"))
    })
    private Location departure;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "iataCode", column = @Column(name = "arr_iata_code")),
            @AttributeOverride(name = "city", column = @Column(name = "arr_city")),
            @AttributeOverride(name = "terminal", column = @Column(name = "arr_terminal")),
            @AttributeOverride(name = "at", column = @Column(name = "arr_at"))
    })
    private Location arrival;

    @Embedded
    private Carrier carrier;

    @Column(name = "flight_number")
    private String flightNumber;

    @Embedded
    private Aircraft aircraft;

    @Embedded
    private Baggage baggage;

    @Column(name = "flight_class")
    private String flightClass;

    @Column(name = "cabin_class")
    private String cabinClass;
}