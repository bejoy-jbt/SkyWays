package com.flightapp.flightservice.service;

import com.flightapp.flightservice.dto.FlightDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FlightSeeder {
    public static List<FlightDto> getSeedData() {
        LocalDate base = LocalDate.now().plusDays(1);
        return List.of(
            build("FA101","New York","Los Angeles", base,    "08:00","11:30","Boeing 737",   28,6, "299.99"),
            build("FA102","Los Angeles","New York",  base,    "14:00","22:30","Boeing 737",   28,6, "319.99"),
            build("FA201","New York","Chicago",      base,    "06:30","08:15","Airbus A320",  30,6, "149.99"),
            build("FA202","Chicago","New York",      base,    "19:00","21:45","Airbus A320",  30,6, "159.99"),
            build("FA301","Los Angeles","Miami",     base,    "09:00","17:30","Boeing 757",   32,6, "249.99"),
            build("FA302","Miami","Los Angeles",     base,    "11:00","14:00","Boeing 757",   32,6, "259.99"),
            build("FA401","New York","London",       base.plusDays(1),"22:00","10:00","Boeing 777",   40,9, "599.99"),
            build("FA402","London","New York",       base.plusDays(1),"13:00","16:00","Boeing 777",   40,9, "649.99"),
            build("FA501","Chicago","Denver",        base,    "07:00","09:30","Airbus A319",  25,6, "129.99"),
            build("FA601","Miami","Atlanta",         base,    "10:00","11:30","Embraer 175",  18,4, "99.99")
        );
    }

    private static FlightDto build(String num, String from, String to,
                                    LocalDate date, String dep, String arr,
                                    String aircraft, int rows, int cols, String price) {
        LocalDate arrDate = arr.compareTo(dep) < 0 ? date.plusDays(1) : date;
        return FlightDto.builder()
                .flightNumber(num).origin(from).destination(to)
                .departureDate(date)
                .departureTime(LocalDateTime.of(date, java.time.LocalTime.parse(dep)))
                .arrivalTime(LocalDateTime.of(arrDate, java.time.LocalTime.parse(arr)))
                .price(new BigDecimal(price)).aircraftType(aircraft)
                .totalRows(rows).seatsPerRow(cols)
                .status("SCHEDULED")
                .build();
    }
}
