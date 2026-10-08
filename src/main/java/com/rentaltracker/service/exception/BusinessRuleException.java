package com.rentaltracker.service.exception;

/** A rule of the app was broken (item not available, rental already closed). Not a database failure. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
