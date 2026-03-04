package com.savadanko.aviasales.flight.mapper;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.dto.FlightOfferResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FlightOfferMapper {
    FlightOfferResponse toDto(FlightOffer flightOffer);
}
