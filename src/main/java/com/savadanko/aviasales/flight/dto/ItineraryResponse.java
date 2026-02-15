package com.savadanko.aviasales.flight.dto;

import com.savadanko.aviasales.flight.Segment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryResponse {
    private Long id;

    private String direction;

    private int durationMinutes;

    private int stops;

    private List<Segment> segments;
}
