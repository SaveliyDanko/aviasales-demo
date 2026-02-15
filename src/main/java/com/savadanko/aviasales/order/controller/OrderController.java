package com.savadanko.aviasales.order.controller;

import com.savadanko.aviasales.order.dto.CheckAvailabilityRequest;
import com.savadanko.aviasales.order.dto.CheckAvailabilityResponse;
import com.savadanko.aviasales.order.service.OrderService;
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
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders/check-availability")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(@Valid @RequestBody CheckAvailabilityRequest request) {
        return ResponseEntity.ok(orderService.checkAvailability(request));
    }
}
