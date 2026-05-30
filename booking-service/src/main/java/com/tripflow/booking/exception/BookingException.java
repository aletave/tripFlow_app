package com.tripflow.booking.exception;

//Eccezioni per il booking-service

public class BookingException extends RuntimeException {
    public BookingException(String message) {
        super(message);
    }
}
