package com.example.travel.exception;

/**
 * Exception thrown when trying to cancel a booking that cannot be cancelled
 */
public class BookingCancellationException extends RuntimeException {
    
    public BookingCancellationException(String message) {
        super(message);
    }
    
    public BookingCancellationException(String message, Throwable cause) {
        super(message, cause);
    }
}