package com.savadanko.aviasales.payment.dto;

import com.savadanko.aviasales.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDataRequest {
    @NotBlank
    private String paymentToken;

    @NotNull
    private Boolean saveCard;

    @NotNull
    private PaymentMethod paymentMethod;
}
