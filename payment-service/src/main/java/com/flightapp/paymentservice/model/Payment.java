package com.flightapp.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bookingId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String stripePaymentIntentId;
    private String stripeClientSecret;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String failureReason;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public enum PaymentStatus { PENDING, SUCCEEDED, FAILED, REFUNDED }
}
