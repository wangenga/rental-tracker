package com.rentaltracker.repository.exception;

import java.sql.SQLException;

import com.rentaltracker.repository.exception.ConstraintViolationException.Type;

public final class SqlExceptionTranslator {
    private SqlExceptionTranslator(){}

    public static RepositoryException translate(String ops, SQLException e){
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("UNIQUE constraint failed"))
            return new ConstraintViolationException(Type.UNIQUE, ops + ": " + msg, e);
        if (msg.contains("NOT_NULL constraint failed"))
            return new ConstraintViolationException(Type.NOT_NULL, ops + ": " + msg, e);
        if (msg.contains("CHECK constraint failed"))
            return new ConstraintViolationException(Type.CHECK, ops + ": " + msg, e);
        if (msg.contains("FOREIGN_KEY constraint failed"))
            return new ConstraintViolationException(Type.FOREIGN_KEY, ops + ": " + msg, e);
        if (msg.contains("constraint failed"))
            return new ConstraintViolationException(Type.OTHER, ops + ": " + msg, e);
        return new RepositoryException(ops + "failed: "+ msg, e);
    }
    
}
