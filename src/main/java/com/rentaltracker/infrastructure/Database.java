package com.rentaltracker.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.rentaltracker.repository.exception.DatabaseConnectionException;
import com.rentaltracker.repository.exception.RepositoryException;

public class Database implements AutoCloseable {
    private final Connection connection;

    public Database(String path) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + path +"?foreign_keys=on");
            DataInitializer.initialize(conn);
            this.connection = conn; 
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException | RuntimeException e) {
            closeQuietly(conn);
            if (e instanceof DatabaseConnectionException dce) {
                throw dce;   // already the right type, don't wrap it twice
            }
            throw new DatabaseConnectionException("Cannot open database at " + path , e);
        }
    }

    public Connection connection() { return connection; }
    
    @Override
    public void close(){
        try { 
            connection.close(); 
        }catch (SQLException e) { throw new RepositoryException("Failed to close database", e);}
    }

    private static void closeQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException ignored) {
            // the original failure is the one worth reporting
        }
    }
}