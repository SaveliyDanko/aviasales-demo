package com.savadanko.aviasales.booking.dto;

import com.savadanko.aviasales.booking.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String bookingId;
    private BookingStatus status;
    private Instant createdAt;
    private String offerId;
    private BigDecimal baseFareAmount;
    private BigDecimal baggageFeeAmount;
    private BigDecimal insuranceFeeAmount;
    private BigDecimal totalAmount;
    private String currency;
}
