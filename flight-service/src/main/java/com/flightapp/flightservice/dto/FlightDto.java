package com.flightapp.flightservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightDto {
    private Long id;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;
    private String aircraftType;
    private int totalRows;
    private int seatsPerRow;
    private long availableSeats;
    private String status;
}
