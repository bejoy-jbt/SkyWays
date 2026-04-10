package com.flightapp.bookingservice.controller;

import com.flightapp.bookingservice.dto.*;
import com.flightapp.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody CreateBookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(email, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingDto>> myBookings(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(bookingService.getMyBookings(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        return ResponseEntity.ok(bookingService.cancelBooking(email, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id) {
        bookingService.deleteBooking(email, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/tickets/{seatNumber}")
    public ResponseEntity<Void> deleteSingleTicket(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id,
            @PathVariable String seatNumber) {
        bookingService.deleteSingleTicket(email, id, seatNumber);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/tickets/passenger/{passengerIndex}")
    public ResponseEntity<Void> deletePassengerTicket(
            @RequestHeader("X-User-Email") String email,
            @PathVariable String id,
            @PathVariable int passengerIndex) {
        bookingService.deletePassengerTicket(email, id, passengerIndex);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<BookingDto>> allBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
}
