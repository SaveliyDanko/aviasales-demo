package com.savadanko.aviasales.booking.service;

import com.savadanko.aviasales.booking.dto.BaggageRequest;
import com.savadanko.aviasales.booking.dto.BookingResponse;
import com.savadanko.aviasales.booking.dto.CreateBookingRequest;
import com.savadanko.aviasales.booking.dto.PassengerRequest;
import com.savadanko.aviasales.booking.entity.BaggageItemEntity;
import com.savadanko.aviasales.booking.entity.BookingEntity;
import com.savadanko.aviasales.booking.entity.BookingStatus;
import com.savadanko.aviasales.booking.entity.ContactInfoEmbeddable;
import com.savadanko.aviasales.booking.entity.InsuranceEmbeddable;
import com.savadanko.aviasales.booking.entity.LoyaltyEmbeddable;
import com.savadanko.aviasales.booking.entity.PassengerDocumentEmbeddable;
import com.savadanko.aviasales.booking.entity.PassengerEntity;
import com.savadanko.aviasales.booking.exception.InvalidBookingException;
import com.savadanko.aviasales.booking.mapper.BookingMapper;
import com.savadanko.aviasales.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        BookingEntity booking = BookingEntity.builder()
                .bookingId(UUID.randomUUID().toString())
                .offerId(request.getOfferId())
                .status(BookingStatus.CREATED)
                .createdAt(Instant.now())
                .contactInfo(new ContactInfoEmbeddable(
                        request.getContactInfo().getEmail(),
                        request.getContactInfo().getPhone()
                ))
                .build();

        if (request.getAncillaries() != null && request.getAncillaries().getInsurance() != null) {
            booking.setInsurance(new InsuranceEmbeddable(
                    request.getAncillaries().getInsurance().getType(),
                    request.getAncillaries().getInsurance().getAccepted()
            ));
        }

        Map<Integer, PassengerEntity> passengersByRequestId = new HashMap<>();
        for (PassengerRequest passengerRequest : request.getPassengers()) {
            PassengerEntity passengerEntity = mapPassenger(passengerRequest);
            booking.addPassenger(passengerEntity);
            passengersByRequestId.put(passengerRequest.getId(), passengerEntity);
        }

        List<BaggageRequest> baggage = request.getAncillaries() != null ? request.getAncillaries().getBaggage() : null;
        if (baggage != null) {
            for (BaggageRequest baggageRequest : baggage) {
                PassengerEntity passenger = passengersByRequestId.get(baggageRequest.getPassengerId());
                if (passenger == null) {
                    throw new InvalidBookingException("Unknown passengerId in baggage: " + baggageRequest.getPassengerId());
                }

                passenger.addBaggageItem(BaggageItemEntity.builder()
                        .count(baggageRequest.getCount())
                        .weight(baggageRequest.getWeight())
                        .build());
            }
        }

        BookingEntity savedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    private PassengerEntity mapPassenger(PassengerRequest request) {
        LoyaltyEmbeddable loyalty = null;
        if (request.getLoyalty() != null) {
            loyalty = new LoyaltyEmbeddable(
                    request.getLoyalty().getAirlineCode(),
                    request.getLoyalty().getNumber()
            );
        }

        return PassengerEntity.builder()
                .passengerId(request.getId())
                .type(request.getType())
                .gender(request.getGender())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .birthDate(request.getBirthDate())
                .citizenship(request.getCitizenship())
                .document(new PassengerDocumentEmbeddable(
                        request.getDocument().getType(),
                        request.getDocument().getNumber()
                ))
                .loyalty(loyalty)
                .build();
    }
}

