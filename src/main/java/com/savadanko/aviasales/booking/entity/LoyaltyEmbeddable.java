package com.savadanko.aviasales.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyEmbeddable {
    @Column(name = "loyalty_airline_code")
    private String airlineCode;

    @Column(name = "loyalty_number")
    private String number;
}

