package com.savadanko.aviasales.order.controller;

import com.savadanko.aviasales.flight.FlightOffer;
import com.savadanko.aviasales.flight.model.Price;
import com.savadanko.aviasales.flight.repository.FlightOfferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightOfferRepository flightOfferRepository;

    @Test
    void checkAvailabilityShouldReturnAvailable() throws Exception {
        String offerId = saveOffer(new BigDecimal("14500.00"), "RUB", true);
        String request = """
                {
                  "offerId": "%s",
                  "expectedPrice": { "amount": 14500.00, "currency": "RUB" }
                }
                """.formatted(offerId);

        mockMvc.perform(post("/api/v1/orders/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.isPriceChanged").value(false))
                .andExpect(jsonPath("$.finalPrice.amount").value(14500.00))
                .andExpect(jsonPath("$.finalPrice.currency").value("RUB"))
                .andExpect(jsonPath("$.bookingToken").exists());
    }

    @Test
    void checkAvailabilityShouldReturnPriceChanged() throws Exception {
        String offerId = saveOffer(new BigDecimal("16200.00"), "RUB", true);
        String request = """
                {
                  "offerId": "%s",
                  "expectedPrice": { "amount": 14500.00, "currency": "RUB" }
                }
                """.formatted(offerId);

        mockMvc.perform(post("/api/v1/orders/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRICE_CHANGED"))
                .andExpect(jsonPath("$.isPriceChanged").value(true))
                .andExpect(jsonPath("$.oldPrice.amount").value(14500.00))
                .andExpect(jsonPath("$.finalPrice.amount").value(16200.00))
                .andExpect(jsonPath("$.difference").value(1700.00))
                .andExpect(jsonPath("$.message").value("Дешевые билеты закончились, доступен следующий тариф."));
    }

    @Test
    void checkAvailabilityShouldReturnSoldOut() throws Exception {
        String offerId = saveOffer(new BigDecimal("14500.00"), "RUB", false);
        String request = """
                {
                  "offerId": "%s",
                  "expectedPrice": { "amount": 14500.00, "currency": "RUB" }
                }
                """.formatted(offerId);

        mockMvc.perform(post("/api/v1/orders/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD_OUT"))
                .andExpect(jsonPath("$.isPriceChanged").value(false))
                .andExpect(jsonPath("$.message").value("Мест на выбранном рейсе больше нет."));
    }

    @Test
    void checkAvailabilityShouldReturnBadRequestWhenOfferIdMissing() throws Exception {
        String request = """
                {
                  "offerId": "",
                  "expectedPrice": { "amount": -10.00, "currency": "RUB" }
                }
                """;

        mockMvc.perform(post("/api/v1/orders/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[*].field",
                        hasItem(containsString("offerId"))))
                .andExpect(jsonPath("$.validationErrors[*].field",
                        hasItem(containsString("expectedPrice.amount"))));
    }

    private String saveOffer(BigDecimal amount, String currency, boolean isBookable) {
        String offerId = "offer-" + UUID.randomUUID();
        FlightOffer offer = FlightOffer.builder()
                .offerId(offerId)
                .source("SYSTEM")
                .isBookable(isBookable)
                .price(new Price(currency, amount, amount, BigDecimal.ZERO))
                .build();
        flightOfferRepository.save(offer);
        return offerId;
    }
}
