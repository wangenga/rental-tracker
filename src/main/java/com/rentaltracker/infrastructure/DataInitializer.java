package com.rentaltracker.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import com.rentaltracker.repository.exception.DatabaseConnectionException;

public final class DataInitializer {

    private DataInitializer() {}

    public static void initialize(Connection conn) {
        try (
            // 1. Get the SQL file from resources
            InputStream inputStream = DataInitializer.class
                    .getClassLoader()
                    .getResourceAsStream("schema.sql")
        ) {
            if (inputStream == null) {
                throw new IllegalStateException("Could not find schema.sql in resources folder!");
            }

            // 2. Read the file content into a String
            String sqlScript;
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                sqlScript = scanner.hasNext() ? scanner.next() : "";
            }

            // 3. Split the script by semicolon (;)
            // Note: This is a simple split. It works for basic DDL.
            String[] statements = sqlScript.split(";");

            // 4. Execute each statement
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    if (!sql.isBlank()) {
                        stmt.execute(sql.trim());// Use execute() for DDL
                        System.out.println("schema.sql ran");
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new DatabaseConnectionException("Failed to initialize database", e);
        }
    }
}