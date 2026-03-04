package com.savadanko.aviasales.booking.mapper;

import com.savadanko.aviasales.booking.dto.BookingResponse;
import com.savadanko.aviasales.booking.entity.BookingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(target = "paymentExpiresAt", ignore = true)
    BookingResponse toResponse(BookingEntity booking);
}
