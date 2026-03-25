package com.flightapp.authservice.controller;

import com.flightapp.authservice.dto.*;
import com.flightapp.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(email);
    }

    // Admin endpoints
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PatchMapping("/admin/users/{id}/toggle")
    public ResponseEntity<UserDto> toggleUser(@PathVariable Long id,
                                               @RequestParam boolean enabled) {
        return ResponseEntity.ok(authService.toggleUser(id, enabled));
    }
}
