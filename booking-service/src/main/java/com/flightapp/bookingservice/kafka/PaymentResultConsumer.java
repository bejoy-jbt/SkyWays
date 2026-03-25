package com.flightapp.bookingservice.kafka;

import com.flightapp.bookingservice.kafka.events.BookingEvent;
import com.flightapp.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {

    private final BookingService bookingService;

    @KafkaListener(topics = "payment.success", groupId = "booking-group")
    public void onPaymentSuccess(BookingEvent event) {
        log.info("Payment SUCCESS received for booking {}", event.getBookingId());
        bookingService.onPaymentSuccess(event);
    }

    @KafkaListener(topics = "payment.failed", groupId = "booking-group")
    public void onPaymentFailed(BookingEvent event) {
        log.info("Payment FAILED received for booking {}", event.getBookingId());
        bookingService.onPaymentFailure(event);
    }
}
