package com.flightapp.flightservice.repository;

import com.flightapp.flightservice.model.Seat;
import com.flightapp.flightservice.model.Seat.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByFlightId(Long flightId);
    List<Seat> findByFlightIdAndStatus(Long flightId, SeatStatus status);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.seatNumber = :seatNumber")
    Optional<Seat> findWithLock(Long flightId, String seatNumber);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flight.id = :flightId AND s.status = 'AVAILABLE'")
    long countAvailable(Long flightId);
}
