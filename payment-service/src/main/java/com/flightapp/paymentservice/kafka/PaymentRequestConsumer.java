package com.flightapp.paymentservice.kafka;

import com.flightapp.paymentservice.kafka.events.BookingEvent;
import com.flightapp.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "payment.requested", groupId = "payment-group")
    public void onPaymentRequested(BookingEvent event) {
        log.info("Received payment.requested for booking {}", event.getBookingId());
        paymentService.processPayment(event);
    }

    @KafkaListener(topics = "payment.refund.requested", groupId = "payment-group")
    public void onRefundRequested(BookingEvent event) {
        log.info("Received payment.refund.requested for booking {}", event.getBookingId());
        paymentService.requestRefund(event);
    }
}
