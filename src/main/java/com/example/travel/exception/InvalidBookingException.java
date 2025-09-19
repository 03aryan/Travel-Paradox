package com.example.travel.exception;

/**
 * Exception thrown when booking dates are invalid or conflicting
 */
public class InvalidBookingException extends RuntimeException {
    
    public InvalidBookingException(String message) {
        super(message);
    }
    
    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}