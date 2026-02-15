package com.savadanko.aviasales.booking.repository;

import com.savadanko.aviasales.booking.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, String> {
}

