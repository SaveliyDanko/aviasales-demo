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
public class ContactInfoEmbeddable {
    @Column(name = "contact_email")
    private String email;

    @Column(name = "contact_phone")
    private String phone;
}
