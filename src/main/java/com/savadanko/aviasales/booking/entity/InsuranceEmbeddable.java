package com.savadanko.aviasales.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceEmbeddable {
    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type")
    private InsuranceType type;

    @Column(name = "insurance_accepted")
    private Boolean accepted;
}
