package com.flightapp.flightservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FlightSearchRequest {
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private int passengers = 1;
}
