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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightDataGenerator {

    private static final int GENERATED_FALLBACK_OFFERS = 5;

    private final FlightOfferRepository flightOfferRepository;
    private final ExternalFlightSourceService externalFlightSourceService;
    private final ResourceLoader resourceLoader;

    @Value("${app.flightdata.csv-path:}")
    private String csvPath;

    @Value("${app.flightdata.csv-enabled:true}")
    private boolean csvEnabled;
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

        List<FlightOffer> all = new ArrayList<>();

        List<FlightOffer> csvOffers = importCsvOffers();
        all.addAll(csvOffers);

        List<FlightOffer> external = importExternalOffers();
        all.addAll(external);

        Map<String, FlightOffer> unique = new LinkedHashMap<>();
        for (FlightOffer o : all) {
            if (o != null && StringUtils.hasText(o.getOfferId())) {
                unique.putIfAbsent(o.getOfferId(), o);
            }
        }
        List<FlightOffer> offers = new ArrayList<>(unique.values());

        int importedCount = offers.size();

        for (int i = 0; i < GENERATED_FALLBACK_OFFERS; i++) {
            offers.add(generateSingleOffer());
        }

        flightOfferRepository.saveAll(offers);
        log.info("Успешно сохранено {} рейсов (CSV+external: {}, локально сгенерировано: {}).",
                offers.size(),
                importedCount,
                GENERATED_FALLBACK_OFFERS
        );
    }

    private List<FlightOffer> importCsvOffers() {
        if (!csvEnabled) {
            log.info("CSV import disabled (app.flightdata.csv-enabled=false).");
            return List.of();
        }
        if (!StringUtils.hasText(csvPath)) {
            log.info("CSV path is empty (app.flightdata.csv-path). Skipping CSV import.");
            return List.of();
        }

        Resource resource = resourceLoader.getResource(csvPath);
        if (!resource.exists()) {
            log.warn("CSV not found at '{}'. Skipping CSV import.", csvPath);
            return List.of();
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            List<FlightOffer> result = new ArrayList<>();

            for (CSVRecord r : parser) {
                try {
                    System.out.println(r);
                    FlightOffer offer = mapCsvRecordToOffer(r);
                    if (offer != null) {
                        result.add(offer);
                    }
                }
                catch (Exception ignored) {}
            }

            log.info("CSV import: loaded {} rows from {}", result.size(), csvPath);
            return result;
        } catch (Exception e) {
            log.warn("CSV import failed from '{}': {}", csvPath, e.getMessage(), e);
            return List.of();
        }
    }

    private FlightOffer mapCsvRecordToOffer(CSVRecord r) {
        String idx = csv(r, "index");
        String airline = firstNonBlank(csv(r, "airline"), AIRLINES.get(random.nextInt(AIRLINES.size())));
        String flight = firstNonBlank(csv(r, "flight"), String.valueOf(1000 + random.nextInt(8999)));
        String sourceCity = firstNonBlank(csv(r, "source_city"), getRandomAirport(null).city());
        String destCity = firstNonBlank(csv(r, "destination_city"), getRandomAirport(null).city());

        if (sourceCity.equalsIgnoreCase(destCity)) {
            destCity = getRandomAirport(null).city();
        }

        String departureTimeRaw = csv(r, "departure_time");
        String arrivalTimeRaw = csv(r, "arrival_time");

        int daysLeft = parseIntOrFallback(csv(r, "days_left"), 1 + random.nextInt(60));
        int stops = parseStops(csv(r, "stops"));
        String flightClass = normalizeClass(csv(r, "class")); // "ECONOMY"/"BUSINESS"
        int durationMins = parseDurationMinutes(csv(r, "duration"), 90 + random.nextInt(200));

        Airport origin = resolveAirportByCity(sourceCity, null);
        Airport destination = resolveAirportByCity(destCity, origin);

        OffsetDateTime depAt = buildDepartureDateTime(daysLeft, departureTimeRaw);

        OffsetDateTime arrAt = depAt.plusMinutes(durationMins);

        arrAt = adjustArrivalIfClockProvided(arrAt, arrivalTimeRaw);

        BigDecimal total = parseBigDecimalOrNull(csv(r, "price"));
        if (total == null) {
            total = estimatePrice(durationMins, stops, flightClass);
        }

        Price price = buildPriceFromTotal(total);

        boolean isBookable = random.nextInt(100) > 10; // 90%
        int totalSeats = 90 + random.nextInt(151);
        int countBookable = isBookable ? (1 + random.nextInt(totalSeats)) : 0;
        Passengers passengers = new Passengers(totalSeats, countBookable);

        String offerId = StringUtils.hasText(idx) ? "csv-" + idx : "csv-" + UUID.randomUUID();
        String source = "CSV_IMPORT";

        FlightOffer offer = FlightOffer.builder()
                .offerId(offerId)
                .source(source)
                .isBookable(countBookable > 0)
                .price(price)
                .passengers(passengers)
                .build();

        Itinerary outbound = buildItineraryFromCsv(
                "OUTBOUND",
                durationMins,
                stops,
                origin,
                destination,
                depAt,
                arrAt,
                airline,
                flight,
                flightClass
        );

        offer.addItinerary(outbound);
        return offer;
    }

    private Itinerary buildItineraryFromCsv(
            String direction,
            int durationMins,
            int stops,
            Airport origin,
            Airport destination,
            OffsetDateTime depAt,
            OffsetDateTime arrAt,
            String airline,
            String flightRaw,
            String flightClass
    ) {
        Itinerary itinerary = Itinerary.builder()
                .direction(direction)
                .durationMinutes(durationMins)
                .stops(Math.max(0, stops))
                .build();

        List<Airport> points = new ArrayList<>();
        points.add(origin);

        Airport last = origin;
        for (int i = 0; i < stops; i++) {
            Airport mid = getRandomAirport(last);
            if (mid.city().equalsIgnoreCase(destination.city())) {
                mid = getRandomAirport(last);
            }
            points.add(mid);
            last = mid;
        }
        points.add(destination);

        int legs = points.size() - 1;
        int baseLeg = Math.max(30, durationMins / legs);
        int remaining = durationMins;

        OffsetDateTime legDep = depAt;

        for (int i = 0; i < legs; i++) {
            Airport dep = points.get(i);
            Airport arr = points.get(i + 1);

            int legMins = (i == legs - 1) ? remaining : clamp(baseLeg + (random.nextInt(21) - 10), 30, remaining - 30 * (legs - i - 1));
            remaining -= legMins;

            OffsetDateTime legArr = legDep.plusMinutes(legMins);

            Plane plane = AIRCRAFTS.get(random.nextInt(AIRCRAFTS.size()));

            Segment segment = Segment.builder()
                    .segmentId("csv-seg-" + UUID.randomUUID().toString().substring(0, 12))
                    .departure(new Location(dep.iata(), dep.city(), dep.terminal(), legDep))
                    .arrival(new Location(arr.iata(), arr.city(), arr.terminal(), legArr))
                    .carrier(new Carrier(airline))
                    .flightNumber(normalizeFlightNumber(flightRaw))
                    .aircraft(new Aircraft(plane.code(), plane.name()))
                    .baggage(buildBaggageByClass(flightClass))
                    .flightClass(flightClass)
                    .cabinClass(mapCabinClass(flightClass))
                    .build();

            itinerary.addSegment(segment);

            legDep = legArr;
        }

        return itinerary;
    }

    private String csv(CSVRecord r, String header) {
        try {
            if (r != null && r.isMapped(header)) {
                String v = r.get(header);
                return StringUtils.hasText(v) ? v.trim() : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private int parseIntOrFallback(String raw, int fallback) {
        if (!StringUtils.hasText(raw)) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal parseBigDecimalOrNull(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            String norm = raw.trim().replace(",", ".");
            return new BigDecimal(norm);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseStops(String raw) {
        if (!StringUtils.hasText(raw)) return 0;

        String v = raw.trim().toLowerCase();
        try {
            return Math.max(0, Integer.parseInt(v));
        } catch (Exception ignored) {}

        if (v.contains("non") || v.contains("zero")) return 0;
        if (v.contains("one") || v.contains("1")) return 1;
        if (v.contains("two") || v.contains("more") || v.contains("2")) return 2;

        return 0;
    }

    private String normalizeClass(String raw) {
        if (!StringUtils.hasText(raw)) {
            return random.nextBoolean() ? "ECONOMY" : "BUSINESS";
        }
        String v = raw.trim().toUpperCase();
        if (v.contains("BUS")) return "BUSINESS";
        return "ECONOMY";
    }

    private int parseDurationMinutes(String raw, int fallback) {
        if (!StringUtils.hasText(raw)) return fallback;

        String v = raw.trim().toLowerCase();

        Pattern p = Pattern.compile("(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?");
        Matcher m = p.matcher(v);
        if (m.matches()) {
            int h = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int mm = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            int mins = h * 60 + mm;
            if (mins > 0) return mins;
        }

        Pattern p2 = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
        Matcher m2 = p2.matcher(v);
        if (m2.matches()) {
            int h = Integer.parseInt(m2.group(1));
            int mm = Integer.parseInt(m2.group(2));
            int mins = h * 60 + mm;
            if (mins > 0) return mins;
        }

        return fallback;
    }

    private OffsetDateTime buildDepartureDateTime(int daysLeft, String departureTimeRaw) {
        ZoneOffset msk = ZoneOffset.ofHours(3);

        LocalTime t = parseLocalTimeOrBucket(departureTimeRaw);
        OffsetDateTime base = OffsetDateTime.now(msk)
                .plusDays(Math.max(1, daysLeft))
                .withHour(t.getHour())
                .withMinute(t.getMinute())
                .withSecond(0)
                .withNano(0)
                .truncatedTo(ChronoUnit.MINUTES);

        return base;
    }

    private LocalTime parseLocalTimeOrBucket(String raw) {
        if (!StringUtils.hasText(raw)) {
            return LocalTime.of(random.nextInt(24), random.nextBoolean() ? 0 : 30);
        }

        String v = raw.trim();

        try {
            if (v.matches("^\\d{1,2}:\\d{2}$")) {
                return LocalTime.parse(v.length() == 4 ? ("0" + v) : v);
            }
        } catch (Exception ignored) {}

        String norm = v.toUpperCase().replace(" ", "_").replace("-", "_");

        int hour;
        switch (norm) {
            case "EARLY_MORNING" -> hour = randBetween(4, 7);
            case "MORNING" -> hour = randBetween(8, 11);
            case "AFTERNOON" -> hour = randBetween(12, 16);
            case "EVENING" -> hour = randBetween(17, 20);
            case "NIGHT" -> hour = randBetween(21, 23);
            case "LATE_NIGHT" -> hour = randBetween(0, 3);
            default -> hour = random.nextInt(24);
        }

        int minute = random.nextBoolean() ? 0 : 30;
        return LocalTime.of(hour, minute);
    }

    private OffsetDateTime adjustArrivalIfClockProvided(OffsetDateTime arrAt, String arrivalTimeRaw) {
        if (!StringUtils.hasText(arrivalTimeRaw)) return arrAt;

        String v = arrivalTimeRaw.trim();
        if (!v.matches("^\\d{1,2}:\\d{2}$")) return arrAt;

        try {
            LocalTime at = LocalTime.parse(v.length() == 4 ? ("0" + v) : v);
            OffsetDateTime candidate = arrAt.withHour(at.getHour()).withMinute(at.getMinute());

            if (candidate.isBefore(arrAt.minusHours(20))) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        } catch (Exception e) {
            return arrAt;
        }
    }

    private Airport resolveAirportByCity(String city, Airport exclude) {
        if (!StringUtils.hasText(city)) {
            return getRandomAirport(exclude);
        }

        for (Airport a : AIRPORTS) {
            if (a.city().equalsIgnoreCase(city.trim())) {
                if (exclude == null || !a.equals(exclude)) {
                    return a;
                }
            }
        }

        String iata = pseudoIata(city);
        Airport synthetic = new Airport(iata, city.trim(), "1");
        if (exclude != null && synthetic.city().equalsIgnoreCase(exclude.city())) {
            return getRandomAirport(exclude);
        }
        return synthetic;
    }

    private String pseudoIata(String city) {
        String letters = city.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (letters.length() >= 3) return letters.substring(0, 3);
        if (letters.length() == 2) return letters + "X";
        if (letters.length() == 1) return letters + "XX";
        return "TST";
    }

    private int randBetween(int min, int max) {
        return min + random.nextInt((max - min) + 1);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String normalizeFlightNumber(String flightRaw) {
        if (!StringUtils.hasText(flightRaw)) {
            return String.valueOf(1000 + random.nextInt(8999));
        }
        return flightRaw.trim();
    }

    private BigDecimal estimatePrice(int durationMins, int stops, String flightClass) {
        double base = 2500.0;
        double perMinute = 18.0;
        double stopPenalty = stops * 800.0;
        double classMultiplier = "BUSINESS".equalsIgnoreCase(flightClass) ? 2.2 : 1.0;

        double raw = (base + durationMins * perMinute + stopPenalty) * classMultiplier;
        raw += random.nextInt(1500) - 500;
        raw = Math.max(3000.0, raw);

        return BigDecimal.valueOf(Math.round(raw));
    }

    private Price buildPriceFromTotal(BigDecimal total) {
        BigDecimal taxes = total.multiply(BigDecimal.valueOf(0.2)).setScale(0, BigDecimal.ROUND_HALF_UP);
        BigDecimal base = total.subtract(taxes);
        return new Price("RUB", total, base, taxes);
    }

    private Baggage buildBaggageByClass(String flightClass) {
        if ("BUSINESS".equalsIgnoreCase(flightClass)) {
            return new Baggage(
                    new BaggageDetails(2, 32, "KG"),
                    new BaggageDetails(1, 12, "KG")
            );
        }
        return new Baggage(
                new BaggageDetails(1, 23, "KG"),
                new BaggageDetails(1, 10, "KG")
        );
    }

    private String mapCabinClass(String flightClass) {
        return "BUSINESS".equalsIgnoreCase(flightClass) ? "C" : "Y";
    }

    private FlightOffer generateSingleOffer() {
        String offerId = UUID.randomUUID().toString();
        boolean isBookable = random.nextInt(100) > 10;

        double totalCost = 3000 + random.nextInt(22000);
        double taxesCost = totalCost * 0.2;
        Price price = new Price("RUB",
                BigDecimal.valueOf(totalCost),
                BigDecimal.valueOf(totalCost - taxesCost),
                BigDecimal.valueOf(taxesCost));

        int totalSeats = 90 + random.nextInt(151);
        int countBookable = isBookable ? (1 + random.nextInt(totalSeats)) : 0;
        Passengers passengers = new Passengers(totalSeats, countBookable);

        FlightOffer offer = FlightOffer.builder()
                .offerId(offerId)
                .source(SOURCES.get(random.nextInt(SOURCES.size())))
                .isBookable(countBookable > 0)
                .price(price)
                .passengers(passengers)
                .build();

        Airport origin = getRandomAirport(null);
        Airport destination = getRandomAirport(origin);

        OffsetDateTime outDepartureTime = OffsetDateTime.now(ZoneOffset.ofHours(3))
                .plusDays(random.nextInt(60) + 1L)
                .plusHours(random.nextInt(24))
                .truncatedTo(ChronoUnit.MINUTES);

        int durationMins = 90 + random.nextInt(200);
        OffsetDateTime outArrivalTime = outDepartureTime.plusMinutes(durationMins);

        Itinerary outbound = generateItinerary("OUTBOUND", durationMins, origin, destination, outDepartureTime, outArrivalTime);
        offer.addItinerary(outbound);

        if (random.nextInt(100) > 20) {
            OffsetDateTime inDepartureTime = outArrivalTime
                    .plusDays(random.nextInt(14) + 2L)
                    .plusHours(random.nextInt(12));
            OffsetDateTime inArrivalTime = inDepartureTime.plusMinutes(durationMins + random.nextInt(20) - 10);

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
                        new BaggageDetails(1, 23, "KG"),
                        new BaggageDetails(1, 10, "KG")
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
