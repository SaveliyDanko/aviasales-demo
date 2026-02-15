package com.savadanko.aviasales.flight;

import com.savadanko.aviasales.flight.model.Passengers;
import com.savadanko.aviasales.flight.model.Price;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flight_offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class FlightOffer {
    @Id
    @Column(name = "offer_id", updatable = false, nullable = false)
    private String offerId;

    @Column(name = "source")
    private String source;

    @Column(name = "is_bookable")
    private boolean isBookable;

    @Embedded
    private Price price;

    @Embedded
    private Passengers passengers;

    @OneToMany(mappedBy = "flightOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Itinerary> itineraries = new ArrayList<>();

    public void addItinerary(Itinerary itinerary) {
        itineraries.add(itinerary);
        itinerary.setFlightOffer(this);
    }
}