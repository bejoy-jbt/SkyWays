package com.flightapp.flightservice.config;

import com.flightapp.flightservice.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final FlightService flightService;

    @Bean
    public ApplicationRunner seedFlights() {
        return args -> flightService.seedIfEmpty();
    }
}
