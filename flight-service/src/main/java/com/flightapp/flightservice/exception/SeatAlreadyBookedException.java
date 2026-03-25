package com.flightapp.flightservice.exception;
public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(String seat) {
        super("Seat " + seat + " is no longer available. Please select another seat.");
    }
}
