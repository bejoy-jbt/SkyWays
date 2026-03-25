package com.flightapp.flightservice.dto;

import lombok.*;

@Data @Builder
public class SeatDto {
    private Long id;
    private String seatNumber;
    private String status;  // AVAILABLE, LOCKED, BOOKED
    private int row;
    private String column;
}
