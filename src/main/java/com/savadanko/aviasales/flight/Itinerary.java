package com.savadanko.aviasales.flight;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itineraries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private FlightOffer flightOffer;

    @Column(name = "direction")
    private String direction;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "stops")
    private int stops;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Segment> segments = new ArrayList<>();

    public void addSegment(Segment segment) {
        segments.add(segment);
        segment.setItinerary(this);
    }
}
