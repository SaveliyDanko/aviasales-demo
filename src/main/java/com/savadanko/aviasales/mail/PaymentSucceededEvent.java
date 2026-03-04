package com.savadanko.aviasales.mail;

public record PaymentSucceededEvent(
        String bookingId,
        String userEmail,
        String transactionId
) {}
