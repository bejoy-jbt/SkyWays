package com.flightapp.bookingservice.service;

import com.flightapp.bookingservice.dto.*;
import com.flightapp.bookingservice.kafka.events.BookingEvent;
import com.flightapp.bookingservice.model.*;
import com.flightapp.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${flight.service.url}")
    private String flightServiceUrl;

    private static final String TOPIC_PAYMENT_REQUESTED = "payment.requested";
    private static final String TOPIC_PAYMENT_REFUND_REQUESTED = "payment.refund.requested";
    private static final String TOPIC_BOOKING_CONFIRMED = "booking.confirmed";
    private static final String TOPIC_BOOKING_FAILED    = "booking.failed";
    private static final String TOPIC_BOOKING_CANCELLED = "booking.cancelled";

    // ── SAGA Step 1: Create booking + lock seat ────────────────────
    @Transactional
    public BookingDto createBooking(String userEmail, CreateBookingRequest req) {
        // Fetch flight info
        Map<?,?> flight = restTemplate.getForObject(
                flightServiceUrl + "/api/flights/" + req.getFlightId(), Map.class);
        if (flight == null) throw new RuntimeException("Flight not found");

        String flightNumber = (String) flight.get("flightNumber");
        BigDecimal price    = new BigDecimal(flight.get("price").toString());

        // Build booking record
        Booking booking = Booking.builder()
                .userEmail(userEmail)
                .flightId(req.getFlightId())
                .flightNumber(flightNumber)
                .seatNumber(req.getSeatNumber())
                .totalAmount(price.multiply(BigDecimal.valueOf(req.getPassengers().size())))
                .status(Booking.BookingStatus.PENDING)
                .passengers(req.getPassengers().stream().map(this::toPassenger).collect(Collectors.toList()))
                .build();

        bookingRepository.save(booking);
        log.info("Booking {} created for {}", booking.getId(), userEmail);

        // Step 2: Lock the seat via flight-service (optimistic locking happens here)
        try {
            Map<String,String> lockReq = Map.of("bookingId", booking.getId(), "seatNumber", req.getSeatNumber());
            restTemplate.postForEntity(
                    flightServiceUrl + "/api/flights/" + req.getFlightId() + "/seats/lock",
                    lockReq, Void.class);

            booking.setStatus(Booking.BookingStatus.SEAT_LOCKED);
            bookingRepository.save(booking);
            log.info("Seat {} locked for booking {}", req.getSeatNumber(), booking.getId());
        } catch (Exception e) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking.setFailureReason("Seat unavailable: " + e.getMessage());
            bookingRepository.save(booking);
            throw new RuntimeException("Failed to lock seat: " + e.getMessage());
        }

        // Step 3: Publish payment.requested event → saga continues in payment-service
        BookingEvent event = BookingEvent.builder()
                .bookingId(booking.getId())
                .userEmail(userEmail)
                .flightId(req.getFlightId())
                .flightNumber(flightNumber)
                .seatNumber(req.getSeatNumber())
                .amount(booking.getTotalAmount())
                .status("PAYMENT_REQUESTED")
                .build();

        kafkaTemplate.send(TOPIC_PAYMENT_REQUESTED, booking.getId(), event);
        booking.setStatus(Booking.BookingStatus.PAYMENT_PROCESSING);
        bookingRepository.save(booking);
        log.info("Payment requested for booking {}, amount={}", booking.getId(), booking.getTotalAmount());

        return toDto(booking);
    }

    // ── SAGA Step 4a: Payment confirmed ───────────────────────────
    @Transactional
    public void onPaymentSuccess(BookingEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresent(booking -> {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setPaymentIntentId(event.getStatus());
            booking.setConfirmedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // Confirm seat permanently
            restTemplate.postForEntity(
                    flightServiceUrl + "/api/flights/" + booking.getFlightId()
                    + "/seats/" + booking.getSeatNumber() + "/confirm", null, Void.class);

            // Notify for email
            kafkaTemplate.send(TOPIC_BOOKING_CONFIRMED, booking.getId(),
                    buildEvent(booking, "CONFIRMED", null));

            log.info("Booking {} CONFIRMED", booking.getId());
        });
    }

    // ── SAGA Step 4b: Payment failed — compensating transaction ───
    @Transactional
    public void onPaymentFailure(BookingEvent event) {
        bookingRepository.findById(event.getBookingId()).ifPresent(booking -> {
            booking.setStatus(Booking.BookingStatus.PAYMENT_FAILED);
            booking.setFailureReason(event.getFailureReason());
            bookingRepository.save(booking);

            // Compensating transaction: release the seat
            try {
                restTemplate.postForEntity(
                        flightServiceUrl + "/api/flights/" + booking.getFlightId()
                        + "/seats/" + booking.getSeatNumber() + "/release", null, Void.class);
                log.info("Seat {} released after payment failure for booking {}",
                        booking.getSeatNumber(), booking.getId());
            } catch (Exception e) {
                log.error("Failed to release seat for booking {}: {}", booking.getId(), e.getMessage());
            }

            kafkaTemplate.send(TOPIC_BOOKING_FAILED, booking.getId(),
                    buildEvent(booking, "PAYMENT_FAILED", event.getFailureReason()));

            log.info("Booking {} PAYMENT_FAILED: {}", booking.getId(), event.getFailureReason());
        });
    }

    // ── Queries ───────────────────────────────────────────────────
    public List<BookingDto> getMyBookings(String email) {
        return bookingRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public BookingDto getById(String id) {
        return toDto(bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Booking not found")));
    }

    // ── User action: Cancel booking ────────────────────────────────
    @Transactional
    public BookingDto cancelBooking(String userEmail, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Not allowed to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            return toDto(booking);
        }
        if (booking.getStatus() == Booking.BookingStatus.PAYMENT_FAILED) {
            // already compensated + seat released, nothing to do
            return toDto(booking);
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled by {}", bookingId, userEmail);

        // Release seat (best-effort)
        try {
            restTemplate.postForEntity(
                    flightServiceUrl + "/api/flights/" + booking.getFlightId()
                            + "/seats/" + booking.getSeatNumber() + "/release",
                    null, Void.class);
            log.info("Seat {} released after cancellation for booking {}", booking.getSeatNumber(), bookingId);
        } catch (Exception e) {
            log.warn("Failed to release seat for cancelled booking {}: {}", bookingId, e.getMessage());
        }

        // Publish cancellation event for email (includes refund messaging)
        kafkaTemplate.send(TOPIC_BOOKING_CANCELLED, bookingId,
                buildEvent(booking, "CANCELLED", "REFUND_INITIATED"));

        // Ask payment-service to mark refund as requested (best-effort)
        kafkaTemplate.send(TOPIC_PAYMENT_REFUND_REQUESTED, bookingId,
                buildEvent(booking, "REFUND_REQUESTED", null));

        return toDto(booking);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private BookingEvent buildEvent(Booking b, String status, String reason) {
        return BookingEvent.builder().bookingId(b.getId()).userEmail(b.getUserEmail())
                .flightId(b.getFlightId()).flightNumber(b.getFlightNumber())
                .seatNumber(b.getSeatNumber()).amount(b.getTotalAmount())
                .status(status).failureReason(reason).build();
    }

    private Passenger toPassenger(PassengerDto dto) {
        return Passenger.builder().firstName(dto.getFirstName()).lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth()).passportNumber(dto.getPassportNumber())
                .nationality(dto.getNationality()).specialRequests(dto.getSpecialRequests()).build();
    }

    private PassengerDto toPassengerDto(Passenger p) {
        PassengerDto dto = new PassengerDto();
        dto.setFirstName(p.getFirstName()); dto.setLastName(p.getLastName());
        dto.setDateOfBirth(p.getDateOfBirth()); dto.setPassportNumber(p.getPassportNumber());
        dto.setNationality(p.getNationality()); dto.setSpecialRequests(p.getSpecialRequests());
        return dto;
    }

    private BookingDto toDto(Booking b) {
        return BookingDto.builder().id(b.getId()).userEmail(b.getUserEmail())
                .flightId(b.getFlightId()).flightNumber(b.getFlightNumber())
                .seatNumber(b.getSeatNumber()).totalAmount(b.getTotalAmount())
                .status(b.getStatus().name()).paymentIntentId(b.getPaymentIntentId())
                .passengers(b.getPassengers().stream().map(this::toPassengerDto).collect(Collectors.toList()))
                .createdAt(b.getCreatedAt()).confirmedAt(b.getConfirmedAt()).build();
    }
}
