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

    @GetMapping("/admin/all")
    public ResponseEntity<List<BookingDto>> allBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
}
