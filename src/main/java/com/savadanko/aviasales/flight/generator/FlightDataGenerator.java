package com.savadanko.aviasales.flight.generator;

import com.savadanko.aviasales.flight.*;
import com.savadanko.aviasales.flight.model.*;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightDataGenerator {

    private final FlightOfferRepository flightOfferRepository;
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

        log.info("Запуск генерации 1000 реалистичных рейсов по РФ...");
        List<FlightOffer> offers = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            offers.add(generateSingleOffer());
        }

        flightOfferRepository.saveAll(offers);
        log.info("Успешно сгенерировано и сохранено 1000 рейсов.");
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
}
