package com.rentaltracker.repository.exception;

public class NotFoundException extends RepositoryException {

    public NotFoundException(String message) {
        super(message);
    }
}