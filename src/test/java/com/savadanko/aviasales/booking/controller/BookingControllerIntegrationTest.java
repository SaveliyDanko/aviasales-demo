package com.savadanko.aviasales.booking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createBookingShouldReturnCreated() throws Exception {
        String request = """
                {
                  "offerId": "offer-f8a9d2-7b1c-4e5a-9012-abcdef123456",
                  "contactInfo": {
                    "email": "ivan.ivanov@example.com",
                    "phone": "+79991234567"
                  },
                  "passengers": [
                    {
                      "id": 1,
                      "type": "ADULT",
                      "gender": "MALE",
                      "firstName": "IVAN",
                      "lastName": "IVANOV",
                      "middleName": "IVANOVICH",
                      "birthDate": "1990-05-15",
                      "citizenship": "RU",
                      "document": {
                        "type": "PASSPORT_RU",
                        "number": "1234 567890"
                      },
                      "loyalty": {
                        "airlineCode": "SU",
                        "number": "SU123456789"
                      }
                    },
                    {
                      "id": 2,
                      "type": "CHILD",
                      "gender": "FEMALE",
                      "firstName": "MARIA",
                      "lastName": "IVANOVA",
                      "birthDate": "2015-10-20",
                      "citizenship": "RU",
                      "document": {
                        "type": "FOREIGN_PASSPORT",
                        "number": "75 1234567"
                      }
                    }
                  ],
                  "ancillaries": {
                    "baggage": [
                      { "passengerId": 1, "count": 1, "weight": 23 },
                      { "passengerId": 2, "count": 0 }
                    ],
                    "insurance": {
                      "type": "MEDICAL_FULL",
                      "accepted": true
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value("offer-f8a9d2-7b1c-4e5a-9012-abcdef123456"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createBookingShouldReturnBadRequestWhenBaggagePassengerUnknown() throws Exception {
        String request = """
                {
                  "offerId": "offer-f8a9d2-7b1c-4e5a-9012-abcdef123456",
                  "contactInfo": {
                    "email": "ivan.ivanov@example.com",
                    "phone": "+79991234567"
                  },
                  "passengers": [
                    {
                      "id": 1,
                      "type": "ADULT",
                      "gender": "MALE",
                      "firstName": "IVAN",
                      "lastName": "IVANOV",
                      "birthDate": "1990-05-15",
                      "citizenship": "RU",
                      "document": {
                        "type": "PASSPORT_RU",
                        "number": "1234 567890"
                      }
                    }
                  ],
                  "ancillaries": {
                    "baggage": [
                      { "passengerId": 99, "count": 1, "weight": 23 }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors[*].message",
                        hasItem(containsString("baggage passengerId must reference existing passenger"))));
    }
}
