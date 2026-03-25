package com.flightapp.flightservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"flight_id", "seatNumber"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String seatNumber; // e.g. "1A", "12C"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private String lockedByBookingId;

    // Optimistic locking — prevents two concurrent transactions booking the same seat
    @Version
    private Long version;

    public enum SeatStatus { AVAILABLE, LOCKED, BOOKED }
}
