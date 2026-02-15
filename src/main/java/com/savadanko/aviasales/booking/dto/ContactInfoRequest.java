package com.savadanko.aviasales.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;
}

