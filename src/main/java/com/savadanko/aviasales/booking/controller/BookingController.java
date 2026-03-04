package com.savadanko.aviasales.booking.controller;

import com.savadanko.aviasales.booking.dto.BookingResponse;
import com.savadanko.aviasales.booking.dto.BookingTimelineEventResponse;
import com.savadanko.aviasales.booking.dto.CreateBookingRequest;
import com.savadanko.aviasales.booking.service.BookingService;
import com.savadanko.aviasales.booking.service.BookingTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingTimelineService bookingTimelineService;

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings/{bookingId}/timeline")
    public ResponseEntity<List<BookingTimelineEventResponse>> getTimeline(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingTimelineService.getBookingTimeline(bookingId));
    }
}
