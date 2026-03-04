package com.savadanko.aviasales.flight.generator;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.Itinerary;
import com.savadanko.aviasales.flight.Segment;
import com.savadanko.aviasales.flight.dto.FlightOfferResponse;
import com.savadanko.aviasales.flight.dto.ItineraryResponse;
import com.savadanko.aviasales.flight.model.Aircraft;
import com.savadanko.aviasales.flight.model.Baggage;
import com.savadanko.aviasales.flight.model.BaggageDetails;
import com.savadanko.aviasales.flight.model.Carrier;
import com.savadanko.aviasales.flight.model.Location;
import com.savadanko.aviasales.flight.model.Passengers;
import com.savadanko.aviasales.flight.model.Price;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import com.savadanko.aviasales.flight.service.ExternalFlightSourceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightDataGenerator {

    private static final int GENERATED_FALLBACK_OFFERS = 5;

    private final FlightOfferRepository flightOfferRepository;
    private final ExternalFlightSourceService externalFlightSourceService;
    private final Random random = new Random();

    // --- Реалистичные словари для РФ ---
    private record Airport(String iata, String city, String terminal) {}
    private final List<Airport> AIRPORTS = List.of(
            new Airport("SVO", "Moscow", "B"),
            new Airport("VKO", "Moscow", "A"),
            new Airport("DME", "Moscow", "1"),
            new Airport("LED", "Saint Petersburg", "1"),
            new Airport("AER", "Sochi", "1"),
            new Airport("KZN", "Kazan", "1A"),
            new Airport("SVX", "Yekaterinburg", "A"),
            new Airport("OVB", "Novosibirsk", "A"),
            new Airport("KGD", "Kaliningrad", "A"),
            new Airport("MRV", "Mineralnye Vody", "A")
    );

    private final List<String> AIRLINES = List.of(
            "Aeroflot", "S7 Airlines", "Ural Airlines", "Pobeda", "Rossiya Airlines", "Utair"
    );

    private record Plane(String code, String name) {}
    private final List<Plane> AIRCRAFTS = List.of(
            new Plane("738", "Boeing 737-800"),
            new Plane("320", "Airbus A320"),
            new Plane("SU9", "Sukhoi Superjet 100")
    );

    private final List<String> SOURCES = List.of("AVIASALES", "YANDEX_AVIA", "TINKOFF_TRAVEL", "DIRECT");

    @PostConstruct
    @Transactional
    public void generateData() {
        if (flightOfferRepository.count() > 0) {
            log.info("База данных уже содержит рейсы. Генерация пропущена.");
            return;
        }

        log.info("Запуск генерации и импорта рейсов...");
        List<FlightOffer> offers = new ArrayList<>(importExternalOffers());
        int importedCount = offers.size();

        for (int i = 0; i < GENERATED_FALLBACK_OFFERS; i++) {
            offers.add(generateSingleOffer());
        }

        flightOfferRepository.saveAll(offers);
        log.info("Успешно сохранено {} рейсов (импортировано: {}, локально сгенерировано: {}).",
                offers.size(),
                importedCount,
                GENERATED_FALLBACK_OFFERS
        );
    }

    private FlightOffer generateSingleOffer() {
        // Базовые параметры
        String offerId = UUID.randomUUID().toString();
        boolean isBookable = random.nextInt(100) > 10; // 90% шанс, что доступно

        // Генерация цены (от 3000 до 25000 рублей)
        double totalCost = 3000 + random.nextInt(22000);
        double taxesCost = totalCost * 0.2; // Налоги примерно 20%
        Price price = new Price("RUB",
                BigDecimal.valueOf(totalCost),
                BigDecimal.valueOf(totalCost - taxesCost),
                BigDecimal.valueOf(taxesCost));

        // Места в самолете
        int totalSeats = 90 + random.nextInt(151); // 90..240
        int countBookable = isBookable ? (1 + random.nextInt(totalSeats)) : 0;
        Passengers passengers = new Passengers(totalSeats, countBookable);

        FlightOffer offer = FlightOffer.builder()
                .offerId(offerId)
                .source(SOURCES.get(random.nextInt(SOURCES.size())))
                .isBookable(countBookable > 0)
                .price(price)
                .passengers(passengers)
                .build();

        // Маршрут
        Airport origin = getRandomAirport(null);
        Airport destination = getRandomAirport(origin); // Чтобы не лететь в тот же город

        // Временные рамки (вылет от 1 до 60 дней вперед)
        OffsetDateTime outDepartureTime = OffsetDateTime.now(ZoneOffset.ofHours(3))
                .plusDays(random.nextInt(60) + 1L)
                .plusHours(random.nextInt(24))
                .truncatedTo(ChronoUnit.MINUTES);

        int durationMins = 90 + random.nextInt(200); // Полет от 1.5 до ~5 часов
        OffsetDateTime outArrivalTime = outDepartureTime.plusMinutes(durationMins);

        // Туда (OUTBOUND)
        Itinerary outbound = generateItinerary("OUTBOUND", durationMins, origin, destination, outDepartureTime, outArrivalTime);
        offer.addItinerary(outbound);

        // Обратно (INBOUND) - 80% рейсов будут туда-обратно
        if (random.nextInt(100) > 20) {
            OffsetDateTime inDepartureTime = outArrivalTime
                    .plusDays(random.nextInt(14) + 2L) // Возврат через 2-15 дней
                    .plusHours(random.nextInt(12));
            OffsetDateTime inArrivalTime = inDepartureTime.plusMinutes(durationMins + random.nextInt(20) - 10); // Чуть другое время в пути

            Itinerary inbound = generateItinerary("INBOUND", durationMins, destination, origin, inDepartureTime, inArrivalTime);
            offer.addItinerary(inbound);
        }

        return offer;
    }

    private Itinerary generateItinerary(String direction, int duration, Airport dep, Airport arr, OffsetDateTime depTime, OffsetDateTime arrTime) {
        Itinerary itinerary = Itinerary.builder()
                .direction(direction)
                .durationMinutes(duration)
                .stops(0)
                .build();

        String airline = AIRLINES.get(random.nextInt(AIRLINES.size()));
        Plane plane = AIRCRAFTS.get(random.nextInt(AIRCRAFTS.size()));
        String flightNumber = String.valueOf(1000 + random.nextInt(8999));

        Segment segment = Segment.builder()
                .segmentId("seg-" + UUID.randomUUID().toString().substring(0, 8))
                .departure(new Location(dep.iata(), dep.city(), dep.terminal(), depTime))
                .arrival(new Location(arr.iata(), arr.city(), arr.terminal(), arrTime))
                .carrier(new Carrier(airline))
                .flightNumber(flightNumber)
                .aircraft(new Aircraft(plane.code(), plane.name()))
                .baggage(new Baggage(
                        new BaggageDetails(1, 23, "KG"), // Багаж
                        new BaggageDetails(1, 10, "KG")  // Ручная кладь
                ))
                .flightClass(random.nextBoolean() ? "ECONOMY" : "BUSINESS")
                .cabinClass(random.nextBoolean() ? "M" : "Y")
                .build();

        itinerary.addSegment(segment);
        return itinerary;
    }

    private Airport getRandomAirport(Airport exclude) {
        Airport selected;
        do {
            selected = AIRPORTS.get(random.nextInt(AIRPORTS.size()));
        } while (selected.equals(exclude));
        return selected;
    }

    private List<FlightOffer> importExternalOffers() {
        List<FlightOfferResponse> externalOffers = externalFlightSourceService.fetchFlights();
        if (externalOffers.isEmpty()) {
            return List.of();
        }

        Map<String, FlightOffer> uniqueOffers = new LinkedHashMap<>();
        for (FlightOfferResponse externalOffer : externalOffers) {
            if (!isValidExternalOffer(externalOffer)) {
                continue;
            }
            if (!uniqueOffers.containsKey(externalOffer.getOfferId())) {
                uniqueOffers.put(externalOffer.getOfferId(), mapExternalOffer(externalOffer));
            }
        }

        return new ArrayList<>(uniqueOffers.values());
    }

    private boolean isValidExternalOffer(FlightOfferResponse offer) {
        return offer != null
                && StringUtils.hasText(offer.getOfferId())
                && offer.getPrice() != null
                && offer.getPrice().getTotal() != null
                && StringUtils.hasText(offer.getPrice().getCurrency());
    }

    private FlightOffer mapExternalOffer(FlightOfferResponse externalOffer) {
        Passengers passengers = normalizePassengers(externalOffer.getPassengers(), externalOffer.isBookable());

        FlightOffer offer = FlightOffer.builder()
                .offerId(externalOffer.getOfferId())
                .source(StringUtils.hasText(externalOffer.getSource()) ? externalOffer.getSource() : "EXTERNAL_SERVICE")
                .isBookable(externalOffer.isBookable() && passengers.getCountBookable() > 0)
                .price(copyPrice(externalOffer.getPrice()))
                .passengers(passengers)
                .build();

        if (externalOffer.getItineraries() == null) {
            return offer;
        }

        for (ItineraryResponse itineraryResponse : externalOffer.getItineraries()) {
            Itinerary itinerary = Itinerary.builder()
                    .direction(itineraryResponse.getDirection())
                    .durationMinutes(itineraryResponse.getDurationMinutes())
                    .stops(itineraryResponse.getStops())
                    .build();

            if (itineraryResponse.getSegments() != null) {
                for (Segment externalSegment : itineraryResponse.getSegments()) {
                    itinerary.addSegment(copySegment(externalSegment));
                }
            }

            offer.addItinerary(itinerary);
        }

        return offer;
    }

    private Passengers normalizePassengers(Passengers source, boolean isBookable) {
        if (source == null) {
            int fallbackBookable = isBookable ? 1 : 0;
            return new Passengers(Math.max(1, fallbackBookable), fallbackBookable);
        }

        int totalSeats = Math.max(0, source.getTotalSeats());
        int countBookable = Math.max(0, source.getCountBookable());
        if (totalSeats == 0 && countBookable > 0) {
            totalSeats = countBookable;
        }
        if (countBookable > totalSeats) {
            countBookable = totalSeats;
        }
        if (totalSeats == 0) {
            totalSeats = 1;
            countBookable = isBookable ? 1 : 0;
        }

        return new Passengers(totalSeats, countBookable);
    }

    private Price copyPrice(Price source) {
        return new Price(
                source.getCurrency(),
                source.getTotal(),
                source.getBase(),
                source.getTaxes()
        );
    }

    private Segment copySegment(Segment source) {
        return Segment.builder()
                .segmentId("ext-seg-" + UUID.randomUUID().toString().substring(0, 12))
                .departure(copyLocation(source.getDeparture()))
                .arrival(copyLocation(source.getArrival()))
                .carrier(copyCarrier(source.getCarrier()))
                .flightNumber(source.getFlightNumber())
                .aircraft(copyAircraft(source.getAircraft()))
                .baggage(copyBaggage(source.getBaggage()))
                .flightClass(source.getFlightClass())
                .cabinClass(source.getCabinClass())
                .build();
    }

    private Location copyLocation(Location source) {
        if (source == null) {
            return null;
        }
        return new Location(source.getIataCode(), source.getCity(), source.getTerminal(), source.getAt());
    }

    private Carrier copyCarrier(Carrier source) {
        if (source == null) {
            return null;
        }
        return new Carrier(source.getOperatingName());
    }

    private Aircraft copyAircraft(Aircraft source) {
        if (source == null) {
            return null;
        }
        return new Aircraft(source.getCode(), source.getName());
    }

    private Baggage copyBaggage(Baggage source) {
        if (source == null) {
            return null;
        }
        return new Baggage(copyBaggageDetails(source.getChecked()), copyBaggageDetails(source.getCabin()));
    }

    private BaggageDetails copyBaggageDetails(BaggageDetails source) {
        if (source == null) {
            return null;
        }
        return new BaggageDetails(source.getAllowance(), source.getWeight(), source.getUnit());
    }
}
