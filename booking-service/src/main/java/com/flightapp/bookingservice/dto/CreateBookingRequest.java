package com.flightapp.bookingservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateBookingRequest {
    @NotNull
    private Long flightId;

    @NotBlank
    private String seatNumber;

    @NotEmpty
    private List<PassengerDto> passengers;
}
