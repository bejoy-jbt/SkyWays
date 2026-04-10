package com.flightapp.bookingservice.service;

import com.flightapp.bookingservice.dto.*;
import com.flightapp.bookingservice.kafka.events.BookingEvent;
import com.flightapp.bookingservice.exception.ForbiddenException;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private static final String TOPIC_BOOKING_TICKET_CANCELLED = "booking.ticket.cancelled";
    private static final Duration DELETE_CUTOFF_BEFORE_DEPARTURE = Duration.ofHours(2);

    // ── SAGA Step 1: Create booking + lock seat ────────────────────
    @Transactional
    public BookingDto createBooking(String userEmail, CreateBookingRequest req) {
        // Fetch flight info
        Map<?,?> flight = restTemplate.getForObject(
                flightServiceUrl + "/api/flights/" + req.getFlightId(), Map.class);
        if (flight == null) throw new RuntimeException("Flight not found");

        String flightNumber = (String) flight.get("flightNumber");
        BigDecimal price    = new BigDecimal(flight.get("price").toString());

        List<String> seatNumbers = normalizeSeatNumbers(req);
        if (seatNumbers.size() != req.getPassengers().size()) {
            throw new RuntimeException("Number of selected seats must match passenger count");
        }

        // Build booking record
        Booking booking = Booking.builder()
                .userEmail(userEmail)
                .flightId(req.getFlightId())
                .flightNumber(flightNumber)
                .seatNumber(seatNumbers.get(0))
                .seatNumbersCsv(String.join(",", seatNumbers))
                .totalAmount(price.multiply(BigDecimal.valueOf(req.getPassengers().size())))
                .status(Booking.BookingStatus.PENDING)
                .passengers(req.getPassengers().stream().map(this::toPassenger).collect(Collectors.toList()))
                .build();

        bookingRepository.save(booking);
        log.info("Booking {} created for {}", booking.getId(), userEmail);

        // Step 2: Lock the seat via flight-service (optimistic locking happens here)
        try {
            for (String seat : seatNumbers) {
                Map<String,String> lockReq = Map.of("bookingId", booking.getId(), "seatNumber", seat);
                restTemplate.postForEntity(
                        flightServiceUrl + "/api/flights/" + req.getFlightId() + "/seats/lock",
                        lockReq, Void.class);
            }

            booking.setStatus(Booking.BookingStatus.SEAT_LOCKED);
            bookingRepository.save(booking);
            log.info("Seats {} locked for booking {}", seatNumbers, booking.getId());
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
                .seatNumber(seatNumbers.get(0))
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
            for (String seat : getSeatNumbers(booking)) {
                restTemplate.postForEntity(
                        flightServiceUrl + "/api/flights/" + booking.getFlightId()
                        + "/seats/" + seat + "/confirm", null, Void.class);
            }

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
                for (String seat : getSeatNumbers(booking)) {
                    restTemplate.postForEntity(
                            flightServiceUrl + "/api/flights/" + booking.getFlightId()
                            + "/seats/" + seat + "/release", null, Void.class);
                }
                log.info("Seats released after payment failure for booking {}", booking.getId());
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
        enforceNotWithinCutoff(booking);

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
            for (String seat : getSeatNumbers(booking)) {
                restTemplate.postForEntity(
                        flightServiceUrl + "/api/flights/" + booking.getFlightId()
                                + "/seats/" + seat + "/release",
                        null, Void.class);
            }
            log.info("Seats released after cancellation for booking {}", bookingId);
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

    @Transactional
    public void deleteBooking(String userEmail, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Not allowed to delete this booking");
        }
        enforceNotWithinCutoff(booking);

        boolean needsCompensation = booking.getStatus() != Booking.BookingStatus.CANCELLED
                && booking.getStatus() != Booking.BookingStatus.PAYMENT_FAILED;

        if (needsCompensation) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            try {
                for (String seat : getSeatNumbers(booking)) {
                    restTemplate.postForEntity(
                            flightServiceUrl + "/api/flights/" + booking.getFlightId()
                                    + "/seats/" + seat + "/release",
                            null, Void.class);
                }
                log.info("Seats released before deleting booking {}", bookingId);
            } catch (Exception e) {
                log.warn("Failed to release seat before deleting booking {}: {}", bookingId, e.getMessage());
            }

            kafkaTemplate.send(TOPIC_BOOKING_CANCELLED, bookingId,
                    buildEvent(booking, "CANCELLED", "REFUND_INITIATED"));
            kafkaTemplate.send(TOPIC_PAYMENT_REFUND_REQUESTED, bookingId,
                    buildEvent(booking, "REFUND_REQUESTED", null));
        }

        bookingRepository.delete(booking);
        log.info("Booking {} deleted by {}", bookingId, userEmail);
    }

    @Transactional
    public void deleteSingleTicket(String userEmail, String bookingId, String seatNumber) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Not allowed to modify this booking");
        }
        enforceNotWithinCutoff(booking);

        List<String> seats = new ArrayList<>(getSeatNumbers(booking));
        int seatIndex = seats.indexOf(seatNumber);
        if (seatIndex < 0) {
            throw new RuntimeException("Seat " + seatNumber + " is not part of this booking");
        }

        if (seats.size() == 1) {
            deleteBooking(userEmail, bookingId);
            return;
        }

        BigDecimal unitFare = booking.getTotalAmount()
                .divide(BigDecimal.valueOf(seats.size()), 2, RoundingMode.HALF_UP);

        try {
            restTemplate.postForEntity(
                    flightServiceUrl + "/api/flights/" + booking.getFlightId()
                            + "/seats/" + seatNumber + "/release",
                    null, Void.class);
            log.info("Seat {} released for partial delete in booking {}", seatNumber, bookingId);
        } catch (Exception e) {
            log.warn("Failed to release seat {} for booking {}: {}", seatNumber, bookingId, e.getMessage());
        }

        seats.remove(seatIndex);
        booking.setSeatNumbersCsv(String.join(",", seats));
        booking.setSeatNumber(seats.get(0));

        List<Passenger> passengers = booking.getPassengers();
        Passenger removedPassenger = null;
        if (passengers != null && !passengers.isEmpty()) {
            int passengerIndex = Math.min(seatIndex, passengers.size() - 1);
            removedPassenger = passengers.remove(passengerIndex);
        }

        BigDecimal newTotal = booking.getTotalAmount().subtract(unitFare);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) newTotal = BigDecimal.ZERO;
        booking.setTotalAmount(newTotal);
        bookingRepository.save(booking);

        if (booking.getStatus() != Booking.BookingStatus.CANCELLED
                && booking.getStatus() != Booking.BookingStatus.PAYMENT_FAILED) {
            BookingEvent refundEvent = BookingEvent.builder()
                    .bookingId(booking.getId())
                    .userEmail(booking.getUserEmail())
                    .flightId(booking.getFlightId())
                    .flightNumber(booking.getFlightNumber())
                    .seatNumber(seatNumber)
                    .amount(unitFare)
                    .status("REFUND_REQUESTED")
                    .build();
            kafkaTemplate.send(TOPIC_PAYMENT_REFUND_REQUESTED, bookingId, refundEvent);
        }

        String passengerName = removedPassenger != null
                ? (removedPassenger.getFirstName() + " " + removedPassenger.getLastName()).trim()
                : null;
        BookingEvent ticketCancelledEvent = BookingEvent.builder()
                .bookingId(booking.getId())
                .userEmail(booking.getUserEmail())
                .flightId(booking.getFlightId())
                .flightNumber(booking.getFlightNumber())
                .seatNumber(seatNumber)
                .amount(unitFare)
                .status("TICKET_CANCELLED")
                .failureReason(passengerName)
                .build();
        kafkaTemplate.send(TOPIC_BOOKING_TICKET_CANCELLED, bookingId, ticketCancelledEvent);

        log.info("Removed seat {} from booking {}. Remaining seats={}, total={}",
                seatNumber, bookingId, seats, booking.getTotalAmount());
    }

    @Transactional
    public void deletePassengerTicket(String userEmail, String bookingId, int passengerIndex) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("Not allowed to modify this booking");
        }
        enforceNotWithinCutoff(booking);
        if (passengerIndex < 0 || passengerIndex >= booking.getPassengers().size()) {
            throw new RuntimeException("Invalid passenger index");
        }
        List<String> seats = getSeatNumbers(booking);
        if (passengerIndex >= seats.size()) {
            throw new RuntimeException("Passenger-seat mapping is invalid for this booking");
        }
        deleteSingleTicket(userEmail, bookingId, seats.get(passengerIndex));
    }

    // ── Helpers ───────────────────────────────────────────────────
    private BookingEvent buildEvent(Booking b, String status, String reason) {
        return BookingEvent.builder().bookingId(b.getId()).userEmail(b.getUserEmail())
                .flightId(b.getFlightId()).flightNumber(b.getFlightNumber())
                .seatNumber(b.getSeatNumber()).amount(b.getTotalAmount())
                .status(status).failureReason(reason).build();
    }

    private void enforceNotWithinCutoff(Booking booking) {
        Instant departure = fetchFlightDepartureInstant(booking.getFlightId());
        Instant cutoff = departure.minus(DELETE_CUTOFF_BEFORE_DEPARTURE);
        Instant now = Instant.now();
        if (!now.isBefore(cutoff)) {
            throw new ForbiddenException("Cannot cancel/delete booking within 2 hours of departure.");
        }
    }

    private Instant fetchFlightDepartureInstant(Long flightId) {
        Map<?, ?> flight = restTemplate.getForObject(
                flightServiceUrl + "/api/flights/" + flightId, Map.class);
        if (flight == null || flight.get("departureTime") == null) {
            throw new RuntimeException("Unable to verify flight departure time");
        }
        String raw = flight.get("departureTime").toString();
        try {
            return java.time.OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
            // Expected format in this project is LocalDateTime like "2026-03-20T06:15:00"
            LocalDateTime ldt = LocalDateTime.parse(raw);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    private List<String> normalizeSeatNumbers(CreateBookingRequest req) {
        List<String> seats = req.getSeatNumbers() != null && !req.getSeatNumbers().isEmpty()
                ? req.getSeatNumbers()
                : List.of(req.getSeatNumber());
        List<String> normalized = seats.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) throw new RuntimeException("At least one seat is required");
        return normalized;
    }

    private List<String> getSeatNumbers(Booking booking) {
        if (booking.getSeatNumbersCsv() == null || booking.getSeatNumbersCsv().isBlank()) {
            return List.of(booking.getSeatNumber());
        }
        return Arrays.stream(booking.getSeatNumbersCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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
                .seatNumbers(getSeatNumbers(b))
                .status(b.getStatus().name()).paymentIntentId(b.getPaymentIntentId())
                .passengers(b.getPassengers().stream().map(this::toPassengerDto).collect(Collectors.toList()))
                .createdAt(b.getCreatedAt()).confirmedAt(b.getConfirmedAt()).build();
    }
}
