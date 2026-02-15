package com.savadanko.aviasales.payment.dto;

import com.savadanko.aviasales.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDataRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotBlank
    private String paymentToken;

    @NotNull
    private Boolean saveCard;

    @NotNull
    private PaymentMethod paymentMethod;
}

