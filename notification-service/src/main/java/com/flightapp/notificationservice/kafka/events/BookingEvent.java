package com.flightapp.notificationservice.kafka.events;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingEvent {
    private String bookingId;
    private String userEmail;
    private Long flightId;
    private String flightNumber;
    private String seatNumber;
    private BigDecimal amount;
    private String status;
    private String failureReason;
}
