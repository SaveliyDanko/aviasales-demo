package com.savadanko.aviasales.payment.dto;

import com.savadanko.aviasales.payment.validation.ValidPaymentProcessRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidPaymentProcessRequest
public class PaymentProcessRequest {
    @NotBlank
    private String bookingId;

    @NotNull
    @Valid
    private PaymentDataRequest payment;

    @NotNull
    @Valid
    private ClientInfoRequest clientInfo;
}

