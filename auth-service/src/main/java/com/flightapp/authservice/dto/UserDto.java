package com.flightapp.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
