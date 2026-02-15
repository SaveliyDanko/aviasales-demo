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
public class PassengerDocumentEmbeddable {
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType type;

    @Column(name = "document_number")
    private String number;
}

