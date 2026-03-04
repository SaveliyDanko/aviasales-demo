package com.savadanko.aviasales.payment.dto;

import com.savadanko.aviasales.booking.entity.BookingStatus;
import com.savadanko.aviasales.payment.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessResponse {
    private PaymentStatus status;
    private String transactionId;
    private BookingStatus bookingStatus;
    private BigDecimal chargedAmount;
    private String chargedCurrency;
    private String errorCode;
    private String message;
}
