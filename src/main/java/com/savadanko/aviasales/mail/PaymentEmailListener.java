package com.savadanko.aviasales.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.userEmail() == null || event.userEmail().isBlank()) {
            log.warn("No user email for bookingId={}, skip sending success email", event.bookingId());
            return;
        }
        emailService.sendPaymentSuccessEmail(event.userEmail(), event.bookingId(), event.transactionId());
    }
}
