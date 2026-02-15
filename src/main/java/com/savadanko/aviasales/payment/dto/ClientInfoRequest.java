package com.savadanko.aviasales.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfoRequest {
    @NotBlank
    @Pattern(
            regexp = "^((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9]{1,3}\\.){3}[0-9]{1,3}))$",
            message = "ipAddress must be a valid IPv4 or IPv6 address"
    )
    private String ipAddress;

    @NotBlank
    private String userAgent;

    @NotBlank
    @URL(message = "returnUrl must be a valid URL")
    private String returnUrl;
}

