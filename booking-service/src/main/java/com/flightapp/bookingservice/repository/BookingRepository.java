package com.flightapp.bookingservice.repository;

import com.flightapp.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByUserEmailOrderByCreatedAtDesc(String email);
    List<Booking> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);
}
