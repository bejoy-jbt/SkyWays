package com.flightapp.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    private String id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false)
    private String flightNumber;

    @Column(nullable = false)
    private String seatNumber;

    @Column(length = 500)
    private String seatNumbersCsv;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    private String paymentIntentId;
    private String failureReason;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "booking_id")
    @Builder.Default
    private List<Passenger> passengers = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    public enum BookingStatus {
        PENDING, SEAT_LOCKED, PAYMENT_PROCESSING, CONFIRMED, PAYMENT_FAILED, CANCELLED
    }
}
