package com.flightapp.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentDto {
    private Long id;
    private String bookingId;
    private String userEmail;
    private BigDecimal amount;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
