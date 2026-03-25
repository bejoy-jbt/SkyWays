package com.flightapp.paymentservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentIntentRequest {
    private String bookingId;
    private BigDecimal amount;
    private String currency;
    private String userEmail;
}
