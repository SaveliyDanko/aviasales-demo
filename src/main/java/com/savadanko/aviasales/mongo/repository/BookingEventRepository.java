package com.savadanko.aviasales.mongo.repository;

import com.savadanko.aviasales.mongo.document.BookingEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingEventRepository extends MongoRepository<BookingEventDocument, String> {
    List<BookingEventDocument> findByBookingIdOrderByCreatedAtAsc(String bookingId);
}
