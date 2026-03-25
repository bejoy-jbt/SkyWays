package com.flightapp.authservice.service;

import com.flightapp.authservice.dto.*;
import com.flightapp.authservice.exception.*;
import com.flightapp.authservice.model.User;
import com.flightapp.authservice.repository.UserRepository;
import com.flightapp.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .role(User.Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isEnabled()) throw new InvalidCredentialsException();
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        log.info("User logged in: {}", user.getEmail());
        return buildResponse(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public UserDto toggleUser(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(enabled);
        return toDto(userRepository.save(user));
    }

    private AuthResponse buildResponse(User user) {
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user))
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId()).email(u.getEmail())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .phone(u.getPhone()).role(u.getRole().name())
                .enabled(u.isEnabled()).createdAt(u.getCreatedAt())
                .build();
    }
}
