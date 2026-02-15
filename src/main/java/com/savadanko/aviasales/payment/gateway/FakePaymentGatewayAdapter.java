package com.savadanko.aviasales.payment.gateway;

import com.savadanko.aviasales.payment.dto.PaymentDataRequest;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGatewayAdapter implements PaymentGatewayAdapter {

    @Override
    public PaymentGatewayResult process(PaymentDataRequest paymentData) {
        String token = paymentData.getPaymentToken() == null ? "" : paymentData.getPaymentToken().toLowerCase();
        if (token.contains("insufficient") || token.contains("fail")) {
            return new PaymentGatewayResult(false, null, "INSUFFICIENT_FUNDS");
        }

        String txSuffix = Integer.toUnsignedString(Math.abs(paymentData.getPaymentToken().hashCode()));
        return new PaymentGatewayResult(true, "tx-" + txSuffix, null);
    }
}
