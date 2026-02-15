package com.savadanko.aviasales.flight.model;

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
public class Aircraft {
    @Column(name = "aircraft_code")
    private String code;
    @Column(name = "aircraft_name")
    private String name;
}
