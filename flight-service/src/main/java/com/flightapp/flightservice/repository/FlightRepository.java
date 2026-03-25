package com.flightapp.flightservice.repository;

import com.flightapp.flightservice.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDate(
            String origin, String destination, LocalDate date);

    @Query("SELECT DISTINCT f.origin FROM Flight f ORDER BY f.origin")
    List<String> findDistinctOrigins();

    @Query("SELECT DISTINCT f.destination FROM Flight f ORDER BY f.destination")
    List<String> findDistinctDestinations();
}
