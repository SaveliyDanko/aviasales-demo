package com.savadanko.aviasales.payment.mapper;

import com.savadanko.aviasales.payment.dto.PaymentProcessResponse;
import com.savadanko.aviasales.payment.entity.PaymentTransactionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentProcessResponse toResponse(PaymentTransactionEntity paymentTransaction);
}

