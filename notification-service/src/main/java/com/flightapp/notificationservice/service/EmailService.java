package com.flightapp.notificationservice.service;

import com.flightapp.notificationservice.kafka.events.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendBookingConfirmation(BookingEvent event) {
        log.info("Sending confirmation email to {}", event.getUserEmail());
        String ref    = event.getBookingId().substring(0, 8).toUpperCase();
        String flight = event.getFlightNumber() != null ? event.getFlightNumber() : "N/A";
        String seat   = event.getSeatNumber()   != null ? event.getSeatNumber()   : "N/A";
        String amount = event.getAmount()        != null ? event.getAmount().toPlainString() : "0";

        String html = buildConfirmationHtml(ref, flight, seat, amount);
        sendHtml(event.getUserEmail(), "Booking Confirmed - " + flight, html);
    }

    public void sendBookingFailure(BookingEvent event) {
        log.info("Sending failure email to {}", event.getUserEmail());
        String ref    = event.getBookingId().substring(0, 8).toUpperCase();
        String flight = event.getFlightNumber() != null ? event.getFlightNumber() : "N/A";
        String reason = event.getFailureReason() != null ? event.getFailureReason() : "Payment declined";

        String html = buildFailureHtml(ref, flight, reason);
        sendHtml(event.getUserEmail(), "Booking Failed - " + flight, html);
    }

    public void sendBookingCancellation(BookingEvent event) {
        log.info("Sending cancellation email to {}", event.getUserEmail());
        String ref    = event.getBookingId().substring(0, 8).toUpperCase();
        String flight = event.getFlightNumber() != null ? event.getFlightNumber() : "N/A";
        String seat   = event.getSeatNumber()   != null ? event.getSeatNumber()   : "N/A";
        String amount = event.getAmount()        != null ? event.getAmount().toPlainString() : "0";

        String html = buildCancellationHtml(ref, flight, seat, amount);
        sendHtml(event.getUserEmail(), "Booking Cancelled - " + flight, html);
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, "FlightApp");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildConfirmationHtml(String ref, String flight, String seat, String amount) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#1d4ed8;padding:24px;text-align:center'>"
            + "<h1 style='color:white;margin:0'>Booking Confirmed!</h1>"
            + "</div>"
            + "<div style='padding:24px;background:#ffffff'>"
            + "<p style='color:#374151;font-size:16px'>Your flight booking has been confirmed and payment processed successfully.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin-top:16px'>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Booking Ref</td>"
            + "<td style='padding:12px;color:#6b7280;font-family:monospace;font-size:16px'>" + ref + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Flight</td>"
            + "<td style='padding:12px;color:#6b7280'>" + flight + "</td>"
            + "</tr>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Seat</td>"
            + "<td style='padding:12px;color:#6b7280'>" + seat + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Amount Paid</td>"
            + "<td style='padding:12px;color:#059669;font-weight:bold;font-size:18px'>$" + amount + " USD</td>"
            + "</tr>"
            + "</table>"
            + "<div style='margin-top:24px;padding:16px;background:#ecfdf5;border-left:4px solid #059669'>"
            + "<p style='margin:0;color:#065f46'>Please arrive at the airport at least 2 hours before departure. Have a great flight!</p>"
            + "</div>"
            + "</div>"
            + "<div style='padding:16px;background:#f9fafb;text-align:center;color:#9ca3af;font-size:12px'>"
            + "FlightApp - Your trusted travel partner"
            + "</div>"
            + "</div>";
    }

    private String buildFailureHtml(String ref, String flight, String reason) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#dc2626;padding:24px;text-align:center'>"
            + "<h1 style='color:white;margin:0'>Booking Failed</h1>"
            + "</div>"
            + "<div style='padding:24px;background:#ffffff'>"
            + "<p style='color:#374151;font-size:16px'>Unfortunately your booking could not be completed.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin-top:16px'>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Booking Ref</td>"
            + "<td style='padding:12px;color:#6b7280;font-family:monospace'>" + ref + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Flight</td>"
            + "<td style='padding:12px;color:#6b7280'>" + flight + "</td>"
            + "</tr>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Reason</td>"
            + "<td style='padding:12px;color:#dc2626'>" + reason + "</td>"
            + "</tr>"
            + "</table>"
            + "<div style='margin-top:24px;padding:16px;background:#fef2f2;border-left:4px solid #dc2626'>"
            + "<p style='margin:0;color:#991b1b'>Your seat has been released. Please try booking again.</p>"
            + "</div>"
            + "</div>"
            + "<div style='padding:16px;background:#f9fafb;text-align:center;color:#9ca3af;font-size:12px'>"
            + "FlightApp - Your trusted travel partner"
            + "</div>"
            + "</div>";
    }

    private String buildCancellationHtml(String ref, String flight, String seat, String amount) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
            + "<div style='background:#111827;padding:24px;text-align:center'>"
            + "<h1 style='color:white;margin:0'>Booking Cancelled</h1>"
            + "</div>"
            + "<div style='padding:24px;background:#ffffff'>"
            + "<p style='color:#374151;font-size:16px'>Your ticket has been cancelled successfully.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin-top:16px'>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Booking Ref</td>"
            + "<td style='padding:12px;color:#6b7280;font-family:monospace;font-size:16px'>" + ref + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Flight</td>"
            + "<td style='padding:12px;color:#6b7280'>" + flight + "</td>"
            + "</tr>"
            + "<tr style='background:#f3f4f6'>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Seat</td>"
            + "<td style='padding:12px;color:#6b7280'>" + seat + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px;font-weight:bold;color:#374151'>Amount</td>"
            + "<td style='padding:12px;color:#111827;font-weight:bold;font-size:18px'>$" + amount + " USD</td>"
            + "</tr>"
            + "</table>"
            + "<div style='margin-top:24px;padding:16px;background:#eff6ff;border-left:4px solid #2563eb'>"
            + "<p style='margin:0;color:#1e3a8a'>Refund initiated: your payment will be refunded to the original payment method (processing time depends on your bank).</p>"
            + "</div>"
            + "</div>"
            + "<div style='padding:16px;background:#f9fafb;text-align:center;color:#9ca3af;font-size:12px'>"
            + "FlightApp - Your trusted travel partner"
            + "</div>"
            + "</div>";
    }
}