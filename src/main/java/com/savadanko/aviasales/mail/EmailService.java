package com.savadanko.aviasales.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendPaymentSuccessEmail(String to, String bookingId, String transactionId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Payment successful — booking " + bookingId);

            String text = """
                    Your payment was successful.

                    Booking: %s
                    Transaction: %s

                    The payment has been completed, expect a ticket in the mail.
                    """.formatted(bookingId, transactionId);

            helper.setText(text, false);
            mailSender.send(message);

            log.info("Payment success email sent to={} bookingId={}", to, bookingId);
        } catch (Exception e) {
            log.error("Failed to send payment success email to={} bookingId={}", to, bookingId, e);
        }
    }
}
