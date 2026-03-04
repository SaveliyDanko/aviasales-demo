package com.savadanko.aviasales.booking.service;

import com.savadanko.aviasales.booking.dto.BaggageRequest;
import com.savadanko.aviasales.booking.dto.BookingResponse;
import com.savadanko.aviasales.booking.dto.CreateBookingRequest;
import com.savadanko.aviasales.booking.dto.InsuranceRequest;
import com.savadanko.aviasales.booking.dto.PassengerRequest;
import com.savadanko.aviasales.booking.entity.BaggageItemEntity;
import com.savadanko.aviasales.booking.entity.BookingEntity;
import com.savadanko.aviasales.booking.entity.BookingStatus;
import com.savadanko.aviasales.booking.entity.ContactInfoEmbeddable;
import com.savadanko.aviasales.booking.entity.InsuranceEmbeddable;
import com.savadanko.aviasales.booking.entity.LoyaltyEmbeddable;
import com.savadanko.aviasales.booking.entity.PassengerDocumentEmbeddable;
import com.savadanko.aviasales.booking.entity.PassengerEntity;
import com.savadanko.aviasales.booking.entity.PassengerType;
import com.savadanko.aviasales.booking.exception.InvalidBookingException;
import com.savadanko.aviasales.booking.mapper.BookingMapper;
import com.savadanko.aviasales.booking.repository.BookingRepository;
import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.model.Passengers;
import com.savadanko.aviasales.flight.model.Price;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import com.savadanko.aviasales.order.dto.MoneyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int MAX_TICKETS_PER_BOOKING = 10;
    private static final BigDecimal BAGGAGE_BASE_FEE_PER_ITEM = BigDecimal.valueOf(1500);
    private static final BigDecimal BAGGAGE_OVERWEIGHT_FEE_PER_KG = BigDecimal.valueOf(200);
    private static final int BAGGAGE_INCLUDED_WEIGHT_KG = 20;
    private static final BigDecimal MEDICAL_INSURANCE_FEE_PER_PASSENGER = BigDecimal.valueOf(490);

    private final BookingRepository bookingRepository;
    private final FlightOfferRepository flightOfferRepository;
    private final BookingMapper bookingMapper;
    private final BookingTimelineService bookingTimelineService;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        FlightOffer offer = flightOfferRepository.findById(request.getOfferId())
                .orElseThrow(() -> new InvalidBookingException("Offer not found: " + request.getOfferId()));
        if (!offer.isBookable()) {
            throw new InvalidBookingException("Offer is no longer bookable: " + request.getOfferId());
        }

        MoneyDto currentOfferPrice = extractCurrentOfferPrice(offer);
        MoneyDto expectedPrice = normalizeMoney(request.getExpectedPrice());
        validateExpectedPrice(expectedPrice, currentOfferPrice, request.getOfferId());
        validateRequestedTicketCount(request.getPassengers(), offer.getPassengers(), request.getOfferId());
        int requestedTickets = request.getPassengers().size();
        BigDecimal baseFareAmount = scaleMoney(
                currentOfferPrice.getAmount().multiply(BigDecimal.valueOf(requestedTickets))
        );

        BookingEntity booking = BookingEntity.builder()
                .bookingId(UUID.randomUUID().toString())
                .offerId(request.getOfferId())
                .status(BookingStatus.CREATED)
                .createdAt(Instant.now())
                .baseFareAmount(baseFareAmount)
                .baggageFeeAmount(ZERO_MONEY)
                .insuranceFeeAmount(ZERO_MONEY)
                .totalAmount(baseFareAmount)
                .currency(currentOfferPrice.getCurrency())
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
        BigDecimal baggageFeeAmount = ZERO_MONEY;
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

                baggageFeeAmount = baggageFeeAmount.add(calculateBaggageFee(baggageRequest));
            }
        }

        BigDecimal insuranceFeeAmount = calculateInsuranceFee(
                request.getAncillaries() == null ? null : request.getAncillaries().getInsurance(),
                booking.getPassengers().size()
        );
        booking.setBaggageFeeAmount(scaleMoney(baggageFeeAmount));
        booking.setInsuranceFeeAmount(scaleMoney(insuranceFeeAmount));
        booking.setTotalAmount(scaleMoney(
                booking.getBaseFareAmount()
                        .add(booking.getBaggageFeeAmount())
                        .add(booking.getInsuranceFeeAmount())
        ));

        BookingEntity savedBooking = bookingRepository.save(booking);
        bookingTimelineService.logBookingCreated(savedBooking);
        return bookingMapper.toResponse(savedBooking);
    }

    private void validateExpectedPrice(MoneyDto expectedPrice, MoneyDto currentPrice, String offerId) {
        boolean sameCurrency = expectedPrice.getCurrency().equals(currentPrice.getCurrency());
        boolean sameAmount = expectedPrice.getAmount().compareTo(currentPrice.getAmount()) == 0;
        if (!sameCurrency || !sameAmount) {
            throw new InvalidBookingException(
                    "Offer price changed for " + offerId + ". Run /orders/check-availability again. " +
                            "Expected " + expectedPrice.getAmount() + " " + expectedPrice.getCurrency() +
                            ", current " + currentPrice.getAmount() + " " + currentPrice.getCurrency()
            );
        }
    }

    private void validateRequestedTicketCount(
            List<PassengerRequest> passengerRequests,
            Passengers availablePassengers,
            String offerId
    ) {
        if (passengerRequests == null || passengerRequests.isEmpty()) {
            throw new InvalidBookingException("At least one passenger is required.");
        }
        if (passengerRequests.size() > MAX_TICKETS_PER_BOOKING) {
            throw new InvalidBookingException("Cannot book more than " + MAX_TICKETS_PER_BOOKING + " tickets per booking.");
        }
        if (availablePassengers == null) {
            throw new InvalidBookingException("Offer has no passenger availability data.");
        }

        int requestedTickets = passengerRequests.size();
        if (requestedTickets > availablePassengers.getCountBookable()) {
            throw new InvalidBookingException(
                    "Not enough available seats for offer " + offerId + ". Requested " + requestedTickets +
                            ", available " + availablePassengers.getCountBookable()
            );
        }
    }

    private MoneyDto extractCurrentOfferPrice(FlightOffer offer) {
        Price price = offer.getPrice();
        if (price == null || price.getTotal() == null || price.getCurrency() == null) {
            throw new InvalidBookingException("Offer has no valid price: " + offer.getOfferId());
        }
        return normalizeMoney(new MoneyDto(price.getTotal(), price.getCurrency()));
    }

    private MoneyDto normalizeMoney(MoneyDto money) {
        if (money == null || money.getAmount() == null || money.getCurrency() == null) {
            throw new InvalidBookingException("Price is required.");
        }
        return new MoneyDto(
                scaleMoney(money.getAmount()),
                money.getCurrency().toUpperCase(Locale.ROOT)
        );
    }

    private BigDecimal calculateBaggageFee(BaggageRequest request) {
        if (request == null || request.getCount() == null || request.getCount() <= 0) {
            return ZERO_MONEY;
        }

        int weight = request.getWeight() == null ? 0 : request.getWeight();
        int overweightKg = Math.max(0, weight - BAGGAGE_INCLUDED_WEIGHT_KG);
        BigDecimal perItemFee = BAGGAGE_BASE_FEE_PER_ITEM.add(
                BAGGAGE_OVERWEIGHT_FEE_PER_KG.multiply(BigDecimal.valueOf(overweightKg))
        );

        return perItemFee.multiply(BigDecimal.valueOf(request.getCount()));
    }

    private BigDecimal calculateInsuranceFee(InsuranceRequest insurance, int passengerCount) {
        if (insurance == null || !Boolean.TRUE.equals(insurance.getAccepted())) {
            return ZERO_MONEY;
        }
        if (insurance.getType() == null) {
            throw new InvalidBookingException("Insurance type is required when insurance is accepted.");
        }

        return switch (insurance.getType()) {
            case MEDICAL_FULL -> MEDICAL_INSURANCE_FEE_PER_PASSENGER.multiply(BigDecimal.valueOf(passengerCount));
        };
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
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
                .type(PassengerType.STANDARD)
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
