package com.example.travel.exception;

/**
 * Exception thrown when a hotel is not found
 */
public class HotelNotFoundException extends RuntimeException {
    
    public HotelNotFoundException(String message) {
        super(message);
    }
    
    public HotelNotFoundException(Long hotelId) {
        super("Hotel not found with ID: " + hotelId);
    }
    
    public HotelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}