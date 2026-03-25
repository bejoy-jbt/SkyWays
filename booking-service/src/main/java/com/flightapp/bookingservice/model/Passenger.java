package com.flightapp.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Passenger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String dateOfBirth;

    private String passportNumber;
    private String nationality;
    private String specialRequests;
}
