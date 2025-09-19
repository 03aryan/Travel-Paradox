package com.example.travel.exception;

/**
 * Exception thrown when user is not authorized to perform an action
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UnauthorizedAccessException forAction(String action) {
        return new UnauthorizedAccessException("You are not authorized to " + action);
    }
}