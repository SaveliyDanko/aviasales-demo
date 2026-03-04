package com.savadanko.aviasales.booking.service;

import com.savadanko.aviasales.booking.dto.BookingTimelineEventResponse;
import com.savadanko.aviasales.booking.entity.BookingEntity;
import com.savadanko.aviasales.mongo.document.BookingEventDocument;
import com.savadanko.aviasales.mongo.repository.BookingEventRepository;
import com.savadanko.aviasales.payment.entity.PaymentTransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingTimelineService {

    private static final String BOOKING_CREATED = "BOOKING_CREATED";
    private static final String PAYMENT_PROCESSED = "PAYMENT_PROCESSED";

    private final BookingEventRepository bookingEventRepository;

    public void logBookingCreated(BookingEntity booking) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("passengersCount", booking.getPassengers() == null ? 0 : booking.getPassengers().size());
            metadata.put("createdAt", booking.getCreatedAt());
            metadata.put("baseFareAmount", booking.getBaseFareAmount());
            metadata.put("baggageFeeAmount", booking.getBaggageFeeAmount());
            metadata.put("insuranceFeeAmount", booking.getInsuranceFeeAmount());
            metadata.put("totalAmount", booking.getTotalAmount());
            metadata.put("currency", booking.getCurrency());

            BookingEventDocument event = BookingEventDocument.builder()
                    .bookingId(booking.getBookingId())
                    .offerId(booking.getOfferId())
                    .eventType(BOOKING_CREATED)
                    .bookingStatus(booking.getStatus() == null ? null : booking.getStatus().name())
                    .message("Booking was created successfully.")
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .build();

            bookingEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Unable to write booking creation event to Mongo. bookingId={}", booking.getBookingId(), e);
        }
    }

    public void logPaymentProcessed(BookingEntity booking, PaymentTransactionEntity paymentTransaction) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            if (paymentTransaction.getErrorCode() != null) {
                metadata.put("errorCode", paymentTransaction.getErrorCode());
            }
            if (paymentTransaction.getPaymentDetails() != null) {
                metadata.put("amount", paymentTransaction.getPaymentDetails().getAmount());
                metadata.put("currency", paymentTransaction.getPaymentDetails().getCurrency());
                metadata.put("paymentMethod", paymentTransaction.getPaymentDetails().getPaymentMethod());
            }

            BookingEventDocument event = BookingEventDocument.builder()
                    .bookingId(booking.getBookingId())
                    .offerId(booking.getOfferId())
                    .eventType(PAYMENT_PROCESSED)
                    .bookingStatus(booking.getStatus() == null ? null : booking.getStatus().name())
                    .paymentStatus(paymentTransaction.getStatus() == null ? null : paymentTransaction.getStatus().name())
                    .transactionId(paymentTransaction.getTransactionId())
                    .message(paymentTransaction.getMessage())
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .build();

            bookingEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Unable to write payment event to Mongo. bookingId={}", booking.getBookingId(), e);
        }
    }

    public List<BookingTimelineEventResponse> getBookingTimeline(String bookingId) {
        return bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(event -> new BookingTimelineEventResponse(
                        event.getEventType(),
                        event.getBookingStatus(),
                        event.getPaymentStatus(),
                        event.getTransactionId(),
                        event.getMessage(),
                        event.getMetadata(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
