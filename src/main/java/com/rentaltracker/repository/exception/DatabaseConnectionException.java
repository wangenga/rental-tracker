package com.rentaltracker.repository.exception;

public class DatabaseConnectionException extends RepositoryException{
    public DatabaseConnectionException(String msg, Throwable cause) 
    { 
        super(msg, cause);
    }
    
}
