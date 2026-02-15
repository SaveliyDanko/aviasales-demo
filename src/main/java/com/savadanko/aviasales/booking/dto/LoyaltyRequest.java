package com.savadanko.aviasales.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyRequest {
    private String airlineCode;
    private String number;
}
