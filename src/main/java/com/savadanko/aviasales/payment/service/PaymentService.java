package com.savadanko.aviasales.payment.service;

import com.savadanko.aviasales.booking.entity.BookingEntity;
import com.savadanko.aviasales.booking.entity.BookingStatus;
import com.savadanko.aviasales.booking.repository.BookingRepository;
import com.savadanko.aviasales.payment.dto.PaymentProcessRequest;
import com.savadanko.aviasales.payment.dto.PaymentProcessResponse;
import com.savadanko.aviasales.payment.entity.ClientInfoEmbeddable;
import com.savadanko.aviasales.payment.entity.PaymentDetailsEmbeddable;
import com.savadanko.aviasales.payment.entity.PaymentStatus;
import com.savadanko.aviasales.payment.entity.PaymentTransactionEntity;
import com.savadanko.aviasales.payment.exception.InvalidPaymentException;
import com.savadanko.aviasales.payment.gateway.PaymentGatewayAdapter;
import com.savadanko.aviasales.payment.gateway.PaymentGatewayResult;
import com.savadanko.aviasales.payment.mapper.PaymentMapper;
import com.savadanko.aviasales.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String SUCCESS_MESSAGE = "The payment has been completed, expect a ticket in the mail.";
    private static final String FAILURE_MESSAGE = "There are not enough funds on the card. Please try another card.";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentProcessResponse processPayment(PaymentProcessRequest request) {
        BookingEntity booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new InvalidPaymentException("Booking not found: " + request.getBookingId()));

        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new InvalidPaymentException("Booking is not in payable state: " + booking.getStatus());
        }

        PaymentGatewayResult gatewayResult = paymentGatewayAdapter.process(request.getPayment());
        PaymentTransactionEntity paymentTransaction = buildBaseEntity(request);

        if (gatewayResult.isSuccess()) {
            booking.setStatus(BookingStatus.TICKETING_IN_PROGRESS);
            bookingRepository.save(booking);

            paymentTransaction.setStatus(PaymentStatus.SUCCESS);
            paymentTransaction.setTransactionId(gatewayResult.getTransactionId());
            paymentTransaction.setMessage(SUCCESS_MESSAGE);

            PaymentTransactionEntity saved = paymentRepository.save(paymentTransaction);
            PaymentProcessResponse response = paymentMapper.toResponse(saved);
            response.setBookingStatus(booking.getStatus());
            return response;
        }

        paymentTransaction.setStatus(PaymentStatus.FAILED);
        paymentTransaction.setTransactionId("tx-failed-" + UUID.randomUUID());
        paymentTransaction.setErrorCode(gatewayResult.getErrorCode());
        paymentTransaction.setMessage(FAILURE_MESSAGE);

        PaymentTransactionEntity saved = paymentRepository.save(paymentTransaction);
        PaymentProcessResponse response = paymentMapper.toResponse(saved);
        response.setTransactionId(null);
        response.setBookingStatus(null);
        return response;
    }

    private PaymentTransactionEntity buildBaseEntity(PaymentProcessRequest request) {
        return PaymentTransactionEntity.builder()
                .bookingId(request.getBookingId())
                .createdAt(Instant.now())
                .paymentDetails(new PaymentDetailsEmbeddable(
                        request.getPayment().getAmount(),
                        request.getPayment().getCurrency(),
                        request.getPayment().getPaymentToken(),
                        request.getPayment().getSaveCard(),
                        request.getPayment().getPaymentMethod()
                ))
                .clientInfo(new ClientInfoEmbeddable(
                        request.getClientInfo().getIpAddress(),
                        request.getClientInfo().getUserAgent(),
                        request.getClientInfo().getReturnUrl()
                ))
                .build();
    }
}
