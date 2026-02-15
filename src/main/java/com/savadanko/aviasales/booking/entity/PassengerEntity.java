package com.savadanko.aviasales.booking.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Integer passengerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type", nullable = false)
    private PassengerType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "citizenship", nullable = false)
    private String citizenship;

    @Embedded
    private PassengerDocumentEmbeddable document;

    @Embedded
    private LoyaltyEmbeddable loyalty;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BaggageItemEntity> baggageItems = new ArrayList<>();

    public void addBaggageItem(BaggageItemEntity baggageItem) {
        baggageItems.add(baggageItem);
        baggageItem.setPassenger(this);
    }
}

