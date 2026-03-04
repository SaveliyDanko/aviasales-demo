package com.savadanko.aviasales.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingTimelineEventResponse {
    private String eventType;
    private String bookingStatus;
    private String paymentStatus;
    private String transactionId;
    private String message;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
