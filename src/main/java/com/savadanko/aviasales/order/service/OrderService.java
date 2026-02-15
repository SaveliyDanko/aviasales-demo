package com.savadanko.aviasales.order.service;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.model.Price;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import com.savadanko.aviasales.order.dto.CheckAvailabilityRequest;
import com.savadanko.aviasales.order.dto.CheckAvailabilityResponse;
import com.savadanko.aviasales.order.dto.CheckAvailabilityStatus;
import com.savadanko.aviasales.order.dto.MoneyDto;
import com.savadanko.aviasales.order.exception.InvalidOrderException;
import com.savadanko.aviasales.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String PRICE_CHANGED_MESSAGE = "Дешевые билеты закончились, доступен следующий тариф.";
    private static final String SOLD_OUT_MESSAGE = "Мест на выбранном рейсе больше нет.";

    private final FlightOfferRepository flightOfferRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {
        log.info("Check order availability: offerId={}", request.getOfferId());

        FlightOffer offer = flightOfferRepository.findById(request.getOfferId()).orElse(null);
        if (offer == null || !offer.isBookable()) {
            return soldOutResponse();
        }

        Price currentPrice = offer.getPrice();
        if (currentPrice == null || currentPrice.getTotal() == null || currentPrice.getCurrency() == null) {
            throw new InvalidOrderException("Offer has no valid price: " + request.getOfferId());
        }

        MoneyDto current = orderMapper.toMoneyDto(currentPrice);
        current.setAmount(scaleMoney(current.getAmount()));
        current.setCurrency(current.getCurrency().toUpperCase(Locale.ROOT));

        MoneyDto expected = new MoneyDto(
                scaleMoney(request.getExpectedPrice().getAmount()),
                request.getExpectedPrice().getCurrency().toUpperCase(Locale.ROOT)
        );

        boolean sameCurrency = expected.getCurrency().equals(current.getCurrency());
        boolean sameAmount = expected.getAmount().compareTo(current.getAmount()) == 0;

        if (sameCurrency && sameAmount) {
            return new CheckAvailabilityResponse(
                    CheckAvailabilityStatus.AVAILABLE,
                    false,
                    null,
                    current,
                    null,
                    "temp-token-" + UUID.randomUUID(),
                    null
            );
        }

        BigDecimal difference = scaleMoney(current.getAmount().subtract(expected.getAmount()));
        return new CheckAvailabilityResponse(
                CheckAvailabilityStatus.PRICE_CHANGED,
                true,
                expected,
                current,
                difference,
                null,
                PRICE_CHANGED_MESSAGE
        );
    }

    private CheckAvailabilityResponse soldOutResponse() {
        return new CheckAvailabilityResponse(
                CheckAvailabilityStatus.SOLD_OUT,
                false,
                null,
                null,
                null,
                null,
                SOLD_OUT_MESSAGE
        );
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

