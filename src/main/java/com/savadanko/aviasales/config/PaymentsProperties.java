package com.savadanko.aviasales.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "payments")
public class PaymentsProperties {
    private Duration bookingPaymentTtl = Duration.ofSeconds(15);
}
