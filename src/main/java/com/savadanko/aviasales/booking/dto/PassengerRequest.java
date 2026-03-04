package com.savadanko.aviasales.booking.dto;

import com.savadanko.aviasales.booking.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRequest {
    @NotNull
    private Integer id;

    @NotNull
    private Gender gender;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String middleName;

    @NotNull
    private LocalDate birthDate;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "citizenship must be a 2-letter code")
    private String citizenship;

    @NotNull
    @Valid
    private PassengerDocumentRequest document;

    @Valid
    private LoyaltyRequest loyalty;
}
