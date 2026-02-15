package com.savadanko.aviasales.payment.controller;

import com.savadanko.aviasales.booking.entity.BookingEntity;
import com.savadanko.aviasales.booking.entity.BookingStatus;
import com.savadanko.aviasales.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void processPaymentShouldReturnSuccess() throws Exception {
        String bookingId = createBookingWithCreatedStatus();

        String request = """
                {
                  "bookingId": "%s",
                  "payment": {
                    "amount": 18500.00,
                    "currency": "RUB",
                    "paymentToken": "tok_12345_encrypted_string_from_bank_widget",
                    "saveCard": true,
                    "paymentMethod": "BANK_CARD"
                  },
                  "clientInfo": {
                    "ipAddress": "192.168.1.55",
                    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X...)",
                    "returnUrl": "https://aviasales-clone.ru/payment/finish"
                  }
                }
                """.formatted(bookingId);

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.bookingStatus").value("TICKETING_IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Оплата прошла, ожидайте билет на почту."));
    }

    @Test
    void processPaymentShouldReturnValidationErrorWhenPaymentTokenMissing() throws Exception {
        String request = """
                {
                  "bookingId": "book-xyz-789-qwe",
                  "payment": {
                    "amount": 18500.00,
                    "currency": "RUB",
                    "saveCard": true,
                    "paymentMethod": "BANK_CARD"
                  },
                  "clientInfo": {
                    "ipAddress": "192.168.1.55",
                    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X...)",
                    "returnUrl": "https://aviasales-clone.ru/payment/finish"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[*].field",
                        hasItem(containsString("payment.paymentToken"))));
    }

    @Test
    void processPaymentShouldReturnValidationErrorWhenSaveCardWithSbp() throws Exception {
        String request = """
                {
                  "bookingId": "book-xyz-789-qwe",
                  "payment": {
                    "amount": 18500.00,
                    "currency": "RUB",
                    "paymentToken": "tok_12345_encrypted_string_from_bank_widget",
                    "saveCard": true,
                    "paymentMethod": "SBP"
                  },
                  "clientInfo": {
                    "ipAddress": "192.168.1.55",
                    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X...)",
                    "returnUrl": "https://aviasales-clone.ru/payment/finish"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[*].message",
                        hasItem(containsString("saveCard is allowed only for BANK_CARD"))));
    }

    private String createBookingWithCreatedStatus() {
        String bookingId = "book-" + UUID.randomUUID();
        BookingEntity booking = BookingEntity.builder()
                .bookingId(bookingId)
                .offerId("offer-" + UUID.randomUUID())
                .status(BookingStatus.CREATED)
                .createdAt(Instant.now())
                .build();
        bookingRepository.save(booking);
        return bookingId;
    }
}
