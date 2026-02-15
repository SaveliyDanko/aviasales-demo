package com.savadanko.aviasales.flight.dto;

import com.savadanko.aviasales.flight.model.Passengers;
import com.savadanko.aviasales.flight.model.Price;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightOfferResponse {
    private String id;

    private String searchId;

    private String source;

    private boolean isBookable;

    private Price price;

    private Passengers passengers;

    private List<ItineraryResponse> itineraries;
}
