package com.savadanko.aviasales.order.mapper;

import com.savadanko.aviasales.flight.model.Price;
import com.savadanko.aviasales.order.dto.MoneyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "amount", source = "total")
    MoneyDto toMoneyDto(Price price);
}
