package com.savadanko.aviasales.booking.mapper;

import com.savadanko.aviasales.booking.dto.BookingResponse;
import com.savadanko.aviasales.booking.entity.BookingEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingResponse toResponse(BookingEntity booking);
}

