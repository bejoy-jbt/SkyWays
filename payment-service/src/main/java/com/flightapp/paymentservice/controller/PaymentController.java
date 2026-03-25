package com.flightapp.paymentservice.controller;

import com.flightapp.paymentservice.dto.PaymentDto;
import com.flightapp.paymentservice.service.PaymentService;
import com.flightapp.paymentservice.service.PaymentService.CardDetails;
import com.flightapp.paymentservice.service.PaymentService.CardValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/validate-card
     * Validates card details — called from the booking page before submitting.
     * Returns { valid, message, cardType, maskedNumber }
     */
    @PostMapping("/validate-card")
    public ResponseEntity<CardValidationResult> validateCard(@RequestBody CardDetails card) {
        return ResponseEntity.ok(paymentService.validateCard(card));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentDto>> myPayments(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(paymentService.getMyPayments(email));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<PaymentDto>> allPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String,String>> health() {
        return ResponseEntity.ok(Map.of("status","UP","service","payment-service"));
    }
}