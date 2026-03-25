package com.flightapp.flightservice.service;

import com.flightapp.flightservice.dto.*;
import com.flightapp.flightservice.exception.SeatAlreadyBookedException;
import com.flightapp.flightservice.model.*;
import com.flightapp.flightservice.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    @Autowired
    private final FlightRepository flightRepository;

    @Autowired
    private final SeatRepository   seatRepository;

    public List<FlightDto> search(FlightSearchRequest req) {
        return flightRepository
                .findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDate(
                        req.getOrigin(), req.getDestination(), req.getDepartureDate())
                .stream()
                .filter(f -> f.getStatus() != Flight.Status.CANCELLED)
                .map(f -> toDto(f, seatRepository.countAvailable(f.getId())))
                .collect(Collectors.toList());
    }

    public List<FlightDto> getAll() {
        return flightRepository.findAll().stream()
                .map(f -> toDto(f, seatRepository.countAvailable(f.getId())))
                .collect(Collectors.toList());
    }

    public FlightDto getById(Long id) {
        Flight f = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + id));
        return toDto(f, seatRepository.countAvailable(f.getId()));
    }

    public List<SeatDto> getSeatMap(Long flightId) {
        return seatRepository.findByFlightId(flightId).stream()
                .map(s -> SeatDto.builder()
                        .id(s.getId())
                        .seatNumber(s.getSeatNumber())
                        .status(s.getStatus().name())
                        .row(Integer.parseInt(s.getSeatNumber().replaceAll("[A-Z]", "")))
                        .column(s.getSeatNumber().replaceAll("[0-9]", ""))
                        .build())
                .sorted(Comparator.comparing(SeatDto::getSeatNumber))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean lockSeat(Long flightId, SeatLockRequest req) {
        try {
            Seat seat = seatRepository.findWithLock(flightId, req.getSeatNumber())
                    .orElseThrow(() -> new RuntimeException("Seat not found"));
            if (seat.getStatus() != Seat.SeatStatus.AVAILABLE)
                throw new SeatAlreadyBookedException(req.getSeatNumber());
            seat.setStatus(Seat.SeatStatus.LOCKED);
            seat.setLockedByBookingId(req.getBookingId());
            seatRepository.save(seat);
            return true;
        } catch (OptimisticLockException e) {
            throw new SeatAlreadyBookedException(req.getSeatNumber());
        }
    }

    @Transactional
    public void confirmSeat(Long flightId, String seatNumber) {
        seatRepository.findWithLock(flightId, seatNumber).ifPresent(s -> {
            s.setStatus(Seat.SeatStatus.BOOKED);
            seatRepository.save(s);
        });
    }

    @Transactional
    public void releaseSeat(Long flightId, String seatNumber) {
        seatRepository.findWithLock(flightId, seatNumber).ifPresent(s -> {
            s.setStatus(Seat.SeatStatus.AVAILABLE);
            s.setLockedByBookingId(null);
            seatRepository.save(s);
        });
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void releaseStaleLockedSeats() {
        log.debug("Stale seat lock cleanup ran");
    }

    @Transactional
    public FlightDto create(FlightDto dto) {
        // Derive departureDate from departureTime if not explicitly provided
        java.time.LocalDate deptDate = dto.getDepartureDate() != null
                ? dto.getDepartureDate()
                : dto.getDepartureTime().toLocalDate();

        Flight f = Flight.builder()
                .flightNumber(dto.getFlightNumber())
                .origin(dto.getOrigin())
                .destination(dto.getDestination())
                .departureDate(deptDate)
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .price(dto.getPrice())
                .aircraftType(dto.getAircraftType())
                .totalRows(dto.getTotalRows())
                .seatsPerRow(dto.getSeatsPerRow())
                .status(Flight.Status.SCHEDULED)
                .build();
        flightRepository.save(f);
        generateSeats(f);
        log.info("Created flight {} with {} seats",
                f.getFlightNumber(), f.getTotalSeats());
        return toDto(f, f.getTotalSeats());
    }

    @Transactional
    public FlightDto update(Long id, FlightDto dto) {
        Flight f = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        f.setPrice(dto.getPrice());
        f.setStatus(Flight.Status.valueOf(dto.getStatus()));
        f.setDepartureTime(dto.getDepartureTime());
        f.setArrivalTime(dto.getArrivalTime());
        flightRepository.save(f);
        return toDto(f, seatRepository.countAvailable(f.getId()));
    }

    @Transactional
    public void delete(Long id) {
        flightRepository.deleteById(id);
    }

    private void generateSeats(Flight flight) {
        String[] cols = {"A","B","C","D","E","F","G","H","I"};
        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= flight.getTotalRows(); row++) {
            for (int col = 0; col < flight.getSeatsPerRow(); col++) {
                seats.add(Seat.builder()
                        .flight(flight)
                        .seatNumber(row + cols[col])
                        .status(Seat.SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
    }

    @Transactional
    public void seedIfEmpty() {
        if (flightRepository.count() > 0) return;
        FlightSeeder.getSeedData().forEach(this::create);
        log.info("Seeded default flights");
    }

    private FlightDto toDto(Flight f, long available) {
        return FlightDto.builder()
                .id(f.getId()).flightNumber(f.getFlightNumber())
                .origin(f.getOrigin()).destination(f.getDestination())
                .departureDate(f.getDepartureDate())
                .departureTime(f.getDepartureTime()).arrivalTime(f.getArrivalTime())
                .price(f.getPrice()).aircraftType(f.getAircraftType())
                .totalRows(f.getTotalRows()).seatsPerRow(f.getSeatsPerRow())
                .availableSeats(available).status(f.getStatus().name())
                .build();
    }
}