package com.savadanko.aviasales.booking.dto;

import com.savadanko.aviasales.booking.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDocumentRequest {
    @NotNull
    private DocumentType type;

    @NotBlank
    private String number;
}

