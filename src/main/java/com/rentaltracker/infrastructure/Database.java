package com.rentaltracker.infrastructure;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    public Database() {
        var url = "jdbc:sqlite:db/rental_tracker.sqlite";

        try (var conn = DriverManager.getConnection(url)) {
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}