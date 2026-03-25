package com.flightapp.flightservice.dto;

import lombok.Data;

@Data
public class SeatLockRequest {
    private String bookingId;
    private String seatNumber;
}
