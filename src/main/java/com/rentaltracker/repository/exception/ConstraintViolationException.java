package com.rentaltracker.repository.exception;

public class ConstraintViolationException extends RepositoryException {
    public enum Type { UNIQUE, NOT_NULL, CHECK, FOREIGN_KEY, OTHER}
    private final Type type;

    public ConstraintViolationException(Type type, String msg, Throwable cause){
        super(msg, cause);
        this.type = type;
    }

    public Type getType(){ return type; }
    
}
