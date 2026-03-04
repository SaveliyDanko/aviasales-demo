package com.savadanko.aviasales.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentsProperties.class)
public class PaymentsConfig {}
