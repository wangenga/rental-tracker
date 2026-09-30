package com.rentaltracker.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.rentaltracker.repository.exception.DatabaseConnectionException;
import com.rentaltracker.repository.exception.RepositoryException;

public class Database implements AutoCloseable {
    private final Connection connection;

    public Database(String path) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path +"?foreign_keys=on");
            DataInitializer.initialize(connection);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Cannot open database at " + path , e);
        }
    }

    public Connection connection() { return connection; }
    
    @Override
    public void close(){
        try { connection.close(); }
        catch (SQLException e) { throw new RepositoryException("Failed to close database", e);}
    }

}