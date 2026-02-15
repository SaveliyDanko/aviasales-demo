package com.savadanko.aviasales.payment.gateway;

import com.savadanko.aviasales.payment.dto.PaymentDataRequest;

public interface PaymentGatewayAdapter {
    PaymentGatewayResult process(PaymentDataRequest paymentData);
}

