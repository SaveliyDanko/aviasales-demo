package com.savadanko.aviasales.payment.controller;

import com.savadanko.aviasales.payment.dto.PaymentProcessRequest;
import com.savadanko.aviasales.payment.dto.PaymentProcessResponse;
import com.savadanko.aviasales.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments/process")
    public ResponseEntity<PaymentProcessResponse> processPayment(@Valid @RequestBody PaymentProcessRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }
}
