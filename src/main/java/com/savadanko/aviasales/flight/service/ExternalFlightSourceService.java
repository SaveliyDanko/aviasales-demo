package com.savadanko.aviasales.flight.service;

import com.savadanko.aviasales.flight.dto.FlightOfferResponse;
import com.savadanko.aviasales.flight.dto.FlightOfferResponseList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ExternalFlightSourceService {

    @Value("${app.flight-import.enabled:false}")
    private boolean importEnabled;

    @Value("${app.flight-import.base-url:}")
    private String externalBaseUrl;

    @Value("${app.flight-import.path:/flights-all}")
    private String externalPath;

    public List<FlightOfferResponse> fetchFlights() {
        if (!importEnabled) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(externalBaseUrl)) {
            log.warn("Flight import is enabled, but app.flight-import.base-url is empty.");
            return Collections.emptyList();
        }

        try {
            FlightOfferResponseList response = RestClient.builder()
                    .baseUrl(externalBaseUrl)
                    .build()
                    .get()
                    .uri(externalPath)
                    .retrieve()
                    .body(FlightOfferResponseList.class);

            if (response == null || response.getContent() == null) {
                log.info("External flight service returned empty response: {}{}", externalBaseUrl, externalPath);
                return Collections.emptyList();
            }

            log.info("Imported {} flights from external service: {}{}", response.getContent().size(), externalBaseUrl, externalPath);
            return response.getContent();
        } catch (Exception e) {
            log.warn("Unable to import flights from external service: {}{}", externalBaseUrl, externalPath, e);
            return Collections.emptyList();
        }
    }
}
