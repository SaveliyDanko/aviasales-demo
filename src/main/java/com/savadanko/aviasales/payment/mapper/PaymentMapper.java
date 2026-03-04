package com.savadanko.aviasales.payment.mapper;

import com.savadanko.aviasales.payment.dto.PaymentProcessResponse;
import com.savadanko.aviasales.payment.entity.PaymentTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "chargedAmount", source = "paymentDetails.amount")
    @Mapping(target = "chargedCurrency", source = "paymentDetails.currency")
    PaymentProcessResponse toResponse(PaymentTransactionEntity paymentTransaction);
}
