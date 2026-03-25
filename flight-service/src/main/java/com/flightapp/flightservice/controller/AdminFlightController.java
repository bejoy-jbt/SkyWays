package com.flightapp.flightservice.controller;

import com.flightapp.flightservice.dto.FlightDto;
import com.flightapp.flightservice.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/flights")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminFlightController {

    private final FlightService flightService;

    @PostMapping
    public ResponseEntity<FlightDto> create(@RequestBody FlightDto dto) {
        return ResponseEntity.ok(flightService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightDto> update(@PathVariable Long id, @RequestBody FlightDto dto) {
        return ResponseEntity.ok(flightService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flightService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
