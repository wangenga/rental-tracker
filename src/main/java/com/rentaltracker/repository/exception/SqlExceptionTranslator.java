package com.rentaltracker.repository.exception;

import java.sql.SQLException;

import org.sqlite.SQLiteException;

import com.rentaltracker.repository.exception.ConstraintViolationException.Type;

public final class SqlExceptionTranslator {
    private SqlExceptionTranslator(){}

    public static RepositoryException translate(String ops, SQLException e){
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (e instanceof SQLiteException se) {
        switch (se.getResultCode().name()) {
            case "SQLITE_CONSTRAINT_UNIQUE", "SQLITE_CONSTRAINT_PRIMARYKEY" ->
                { return new ConstraintViolationException(Type.UNIQUE, ops + ": " + msg, e); }
            case "SQLITE_CONSTRAINT_NOTNULL" ->
                { return new ConstraintViolationException(Type.NOT_NULL, ops + ": " + msg, e); }
            case "SQLITE_CONSTRAINT_CHECK" ->
                { return new ConstraintViolationException(Type.CHECK, ops + ": " + msg, e); }
            case "SQLITE_CONSTRAINT_FOREIGNKEY" ->
                { return new ConstraintViolationException(Type.FOREIGN_KEY, ops + ": " + msg, e); }
            default -> { }
        }
        if (se.getResultCode().name().startsWith("SQLITE_CONSTRAINT")) {
            return new ConstraintViolationException(Type.OTHER, ops + ": " + msg, e);
        }
    }
        return new RepositoryException(ops + " failed: "+ msg, e);
    }
    
}
