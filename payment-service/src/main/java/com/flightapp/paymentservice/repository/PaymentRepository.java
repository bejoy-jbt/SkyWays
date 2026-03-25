package com.flightapp.paymentservice.repository;

import com.flightapp.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(String bookingId);
    List<Payment> findByUserEmail(String email);
    List<Payment> findAllByOrderByCreatedAtDesc();
}
