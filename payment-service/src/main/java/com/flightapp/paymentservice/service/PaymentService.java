package com.flightapp.paymentservice.service;

import com.flightapp.paymentservice.dto.PaymentDto;
import com.flightapp.paymentservice.kafka.events.BookingEvent;
import com.flightapp.paymentservice.model.Payment;
import com.flightapp.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    private static final String TOPIC_SUCCESS = "payment.success";
    private static final String TOPIC_FAILED  = "payment.failed";

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        boolean isTest = stripeSecretKey.startsWith("sk_test");
        log.info("Stripe initialised. Test mode: {}", isTest);
        if (stripeSecretKey.equals("sk_test_placeholder")) {
            log.warn("⚠️  Stripe key is placeholder — payments will AUTO-APPROVE without calling Stripe");
        }
    }

    @Transactional
    public void processPayment(BookingEvent event) {
        log.info("Processing payment for booking {} amount={}", event.getBookingId(), event.getAmount());

        Payment payment = Payment.builder()
                .bookingId(event.getBookingId())
                .userEmail(event.getUserEmail())
                .amount(event.getAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // If real Stripe key is configured — use Stripe
        // If placeholder — auto-approve (for development)
        if (!stripeSecretKey.equals("sk_test_placeholder")) {
            processWithStripe(event, payment);
        } else {
            processAutoApprove(event, payment);
        }
    }

    @Transactional
    public void requestRefund(BookingEvent event) {
        paymentRepository.findByBookingId(event.getBookingId()).ifPresentOrElse(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
                log.info("Refund already applied for booking {}", event.getBookingId());
                return;
            }
            if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED) {
                log.info("Skipping refund for booking {} because payment status is {}",
                        event.getBookingId(), payment.getStatus());
                return;
            }

            // This project currently runs Stripe payments in auto-confirm mode (or auto-approve in dev).
            // For now we mark as refunded in our DB; Stripe refund integration can be added later.
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Marked payment as REFUNDED for booking {}", event.getBookingId());
        }, () -> log.warn("Refund requested but no payment found for booking {}", event.getBookingId()));
    }

    private void processWithStripe(BookingEvent event, Payment payment) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(event.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("usd")
                    .setDescription("Flight booking " + event.getBookingId())
                    .putMetadata("bookingId",    event.getBookingId())
                    .putMetadata("flightNumber", event.getFlightNumber())
                    .putMetadata("seatNumber",   event.getSeatNumber())
                    .setConfirm(true)
                    .setPaymentMethod("pm_card_visa")
                    .setReturnUrl("http://localhost:3000/booking/confirm")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            payment.setStripePaymentIntentId(intent.getId());
            payment.setStripeClientSecret(intent.getClientSecret());
            payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("✅ Stripe payment SUCCEEDED for booking {} intentId={}",
                    event.getBookingId(), intent.getId());

            publishSuccess(event, intent.getId());

        } catch (StripeException e) {
            log.error("❌ Stripe payment FAILED for booking {}: {}", event.getBookingId(), e.getMessage());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            publishFailure(event, e.getMessage());
        }
    }

    private void processAutoApprove(BookingEvent event, Payment payment) {
        String ref = "pay_dev_" + System.currentTimeMillis();
        payment.setStripePaymentIntentId(ref);
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("✅ Auto-approved payment for booking {} ref={}", event.getBookingId(), ref);
        publishSuccess(event, ref);
    }

    private void publishSuccess(BookingEvent event, String intentId) {
        BookingEvent success = BookingEvent.builder()
                .bookingId(event.getBookingId())
                .userEmail(event.getUserEmail())
                .flightId(event.getFlightId())
                .flightNumber(event.getFlightNumber())
                .seatNumber(event.getSeatNumber())
                .amount(event.getAmount())
                .status(intentId)
                .build();
        kafkaTemplate.send(TOPIC_SUCCESS, event.getBookingId(), success);
        log.info("Published payment.success for booking {}", event.getBookingId());
    }

    private void publishFailure(BookingEvent event, String reason) {
        BookingEvent failed = BookingEvent.builder()
                .bookingId(event.getBookingId())
                .userEmail(event.getUserEmail())
                .flightId(event.getFlightId())
                .flightNumber(event.getFlightNumber())
                .seatNumber(event.getSeatNumber())
                .amount(event.getAmount())
                .status("PAYMENT_FAILED")
                .failureReason(reason)
                .build();
        kafkaTemplate.send(TOPIC_FAILED, event.getBookingId(), failed);
        log.info("Published payment.failed for booking {}", event.getBookingId());
    }

    // ── Card validation endpoint ────────────────────────────────────
    public CardValidationResult validateCard(CardDetails card) {
        String number = card.getNumber().replaceAll("\\s+", "");
        String expiry = card.getExpiry().trim();
        String cvc    = card.getCvc().trim();

        if (number.length() < 13 || number.length() > 19)
            return CardValidationResult.fail("Card number must be 13–19 digits");
        if (!number.matches("\\d+"))
            return CardValidationResult.fail("Card number must contain digits only");
        if (!luhn(number))
            return CardValidationResult.fail("Invalid card number");
        if (!expiry.matches("(0[1-9]|1[0-2])/(\\d{2}|\\d{4})"))
            return CardValidationResult.fail("Expiry must be MM/YY");

        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year  = Integer.parseInt(parts[1]);
        if (year < 100) year += 2000;
        java.time.YearMonth cardExpiry = java.time.YearMonth.of(year, month);
        if (cardExpiry.isBefore(java.time.YearMonth.now()))
            return CardValidationResult.fail("Card has expired");
        if (!cvc.matches("\\d{3,4}"))
            return CardValidationResult.fail("CVC must be 3 or 4 digits");

        return CardValidationResult.success(detectCardType(number),
                "**** **** **** " + number.substring(number.length() - 4));
    }

    private boolean luhn(String number) {
        int sum = 0; boolean alt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alt) { n *= 2; if (n > 9) n -= 9; }
            sum += n; alt = !alt;
        }
        return sum % 10 == 0;
    }

    private String detectCardType(String n) {
        if (n.startsWith("4"))              return "Visa";
        if (n.matches("5[1-5]\\d+"))        return "Mastercard";
        if (n.matches("2[2-7]\\d+"))        return "Mastercard";
        if (n.matches("3[47]\\d+"))         return "Amex";
        if (n.matches("6(?:011|5\\d{2})\\d+")) return "Discover";
        return "Card";
    }

    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<PaymentDto> getMyPayments(String email) {
        return paymentRepository.findByUserEmail(email)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId()).bookingId(p.getBookingId()).userEmail(p.getUserEmail())
                .amount(p.getAmount()).stripePaymentIntentId(p.getStripePaymentIntentId())
                .status(p.getStatus().name()).failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt()).processedAt(p.getProcessedAt())
                .build();
    }

    public static class CardDetails {
        private String number, expiry, cvc, holderName;
        public String getNumber()     { return number; }
        public String getExpiry()     { return expiry; }
        public String getCvc()        { return cvc; }
        public String getHolderName() { return holderName; }
        public void setNumber(String v)     { this.number = v; }
        public void setExpiry(String v)     { this.expiry = v; }
        public void setCvc(String v)        { this.cvc = v; }
        public void setHolderName(String v) { this.holderName = v; }
    }

    public static class CardValidationResult {
        private boolean valid; private String message, cardType, maskedNumber;
        public static CardValidationResult success(String type, String masked) {
            CardValidationResult r = new CardValidationResult();
            r.valid = true; r.message = "Card valid"; r.cardType = type; r.maskedNumber = masked; return r;
        }
        public static CardValidationResult fail(String msg) {
            CardValidationResult r = new CardValidationResult();
            r.valid = false; r.message = msg; return r;
        }
        public boolean isValid()        { return valid; }
        public String getMessage()      { return message; }
        public String getCardType()     { return cardType; }
        public String getMaskedNumber() { return maskedNumber; }
    }
}