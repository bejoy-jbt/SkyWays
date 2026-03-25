package com.flightapp.bookingservice.dto;

import lombok.Data;

@Data
public class PassengerDto {
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String passportNumber;
    private String nationality;
    private String specialRequests;
}
