package com.rentaltracker.infrastructure;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DataInitializer {

    public static void initialize() {
        // 1. Get the SQL file from resources
        InputStream inputStream = DataInitializer.class
                .getClassLoader()
                .getResourceAsStream("schema.sql");

        if (inputStream == null) {
            throw new RuntimeException("Could not find schema.sql in resources folder!");
        }

        // 2. Read the file content into a String
        String sqlScript;
        try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
            sqlScript = scanner.hasNext() ? scanner.next() : "";
        }

        // 3. Split the script by semicolon (;)
        // Note: This is a simple split. It works for basic DDL.
        String[] statements = sqlScript.split(";");

        // 4. Execute each statement
        try (var conn = DriverManager.getConnection("jdbc:sqlite:db/rental_tracker.sqlite?foreign_keys=on");
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim()); // Use execute() for DDL
                }
            }
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}