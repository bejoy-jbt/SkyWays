package com.flightapp.flightservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Flight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flightNumber;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String aircraftType; // e.g. "Boeing 737"

    private int totalRows;
    private int seatsPerRow;  // total = rows * seatsPerRow

    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public int getTotalSeats() { return totalRows * seatsPerRow; }

    public enum Status { SCHEDULED, BOARDING, DEPARTED, ARRIVED, CANCELLED }
}
