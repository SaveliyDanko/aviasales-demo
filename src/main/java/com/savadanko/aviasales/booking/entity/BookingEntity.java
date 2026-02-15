package com.savadanko.aviasales.booking.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity {
    @Id
    @Column(name = "booking_id", nullable = false, updatable = false)
    private String bookingId;

    @Column(name = "offer_id", nullable = false)
    private String offerId;

    @Embedded
    private ContactInfoEmbeddable contactInfo;

    @Embedded
    private InsuranceEmbeddable insurance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PassengerEntity> passengers = new ArrayList<>();

    public void addPassenger(PassengerEntity passenger) {
        passengers.add(passenger);
        passenger.setBooking(this);
    }
}

