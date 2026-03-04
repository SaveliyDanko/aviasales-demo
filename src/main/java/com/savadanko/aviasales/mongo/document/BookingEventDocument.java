package com.savadanko.aviasales.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "booking_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEventDocument {

    @Id
    private String id;

    @Indexed
    private String bookingId;

    @Indexed
    private String offerId;

    private String eventType;
    private String bookingStatus;
    private String paymentStatus;
    private String transactionId;
    private String message;

    private Map<String, Object> metadata;

    @Indexed(direction = IndexDirection.ASCENDING)
    private Instant createdAt;
}
