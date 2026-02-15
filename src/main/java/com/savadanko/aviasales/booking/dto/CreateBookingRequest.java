package com.savadanko.aviasales.booking.dto;

import com.savadanko.aviasales.booking.validation.ValidBookingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidBookingRequest
public class CreateBookingRequest {
    @NotBlank
    private String offerId;

    @NotNull
    @Valid
    private ContactInfoRequest contactInfo;

    @NotEmpty
    @Valid
    private List<PassengerRequest> passengers;

    @Valid
    private AncillariesRequest ancillaries;
}

