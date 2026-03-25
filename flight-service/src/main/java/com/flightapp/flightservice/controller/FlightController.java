package com.flightapp.flightservice.controller;

import com.flightapp.flightservice.dto.*;
import com.flightapp.flightservice.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FlightController {

    private final FlightService flightService;

    @PostMapping("/search")
    public ResponseEntity<List<FlightDto>> search(@RequestBody FlightSearchRequest req) {
        return ResponseEntity.ok(flightService.search(req));
    }

    @GetMapping
    public ResponseEntity<List<FlightDto>> getAll() {
        return ResponseEntity.ok(flightService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatDto>> getSeatMap(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getSeatMap(id));
    }

    // Called internally by booking-service
    @PostMapping("/{id}/seats/lock")
    public ResponseEntity<Void> lockSeat(@PathVariable Long id,
                                          @RequestBody SeatLockRequest req) {
        flightService.lockSeat(id, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/seats/{seatNumber}/confirm")
    public ResponseEntity<Void> confirmSeat(@PathVariable Long id,
                                             @PathVariable String seatNumber) {
        flightService.confirmSeat(id, seatNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/seats/{seatNumber}/release")
    public ResponseEntity<Void> releaseSeat(@PathVariable Long id,
                                             @PathVariable String seatNumber) {
        flightService.releaseSeat(id, seatNumber);
        return ResponseEntity.ok().build();
    }
}
