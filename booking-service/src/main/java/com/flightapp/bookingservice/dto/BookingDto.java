package com.flightapp.bookingservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class BookingDto {
    private String id;
    private String userEmail;
    private Long flightId;
    private String flightNumber;
    private String seatNumber;
    private BigDecimal totalAmount;
    private String status;
    private String paymentIntentId;
    private List<PassengerDto> passengers;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
