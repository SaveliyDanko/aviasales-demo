package com.savadanko.aviasales.flight.dto;

import com.savadanko.aviasales.flight.model.Aircraft;
import com.savadanko.aviasales.flight.model.Baggage;
import com.savadanko.aviasales.flight.model.Carrier;
import com.savadanko.aviasales.flight.model.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentResponse {
    private String segmentId;

    private Location departure;

    private Location arrival;

    private Carrier carrier;

    private String flightNumber;

    private Aircraft aircraft;

    private Baggage baggage;

    private String flightClass;

    private String cabinClass;
}
