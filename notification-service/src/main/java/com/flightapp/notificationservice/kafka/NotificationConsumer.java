package com.flightapp.notificationservice.kafka;

import com.flightapp.notificationservice.kafka.events.BookingEvent;
import com.flightapp.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "booking.confirmed", groupId = "notification-group")
    public void onBookingConfirmed(BookingEvent event) {
        log.info("Sending confirmation email for booking {} to {}",
                event.getBookingId(), event.getUserEmail());
        emailService.sendBookingConfirmation(event);
    }

    @KafkaListener(topics = "booking.failed", groupId = "notification-group")
    public void onBookingFailed(BookingEvent event) {
        log.info("Sending failure email for booking {} to {}",
                event.getBookingId(), event.getUserEmail());
        emailService.sendBookingFailure(event);
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "notification-group")
    public void onBookingCancelled(BookingEvent event) {
        log.info("Sending cancellation email for booking {} to {}",
                event.getBookingId(), event.getUserEmail());
        emailService.sendBookingCancellation(event);
    }

    @KafkaListener(topics = "booking.ticket.cancelled", groupId = "notification-group")
    public void onBookingTicketCancelled(BookingEvent event) {
        log.info("Sending ticket cancellation email for booking {} to {}",
                event.getBookingId(), event.getUserEmail());
        emailService.sendTicketCancellation(event);
    }
}
