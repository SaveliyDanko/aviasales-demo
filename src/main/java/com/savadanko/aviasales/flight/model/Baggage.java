package com.savadanko.aviasales.flight.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Baggage {
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "allowance", column = @Column(name = "baggage_checked_allowance")),
            @AttributeOverride(name = "weight", column = @Column(name = "baggage_checked_weight")),
            @AttributeOverride(name = "unit", column = @Column(name = "baggage_checked_unit"))
    })
    private BaggageDetails checked;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "allowance", column = @Column(name = "baggage_cabin_allowance")),
            @AttributeOverride(name = "weight", column = @Column(name = "baggage_cabin_weight")),
            @AttributeOverride(name = "unit", column = @Column(name = "baggage_cabin_unit"))
    })
    private BaggageDetails cabin;
}
