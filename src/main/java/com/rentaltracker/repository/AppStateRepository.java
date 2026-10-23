package com.rentaltracker.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AppStateRepository {

    private final Connection connection;

    public AppStateRepository(Connection connection) {
        this.connection = connection;
    }

    public Optional<String> get(String key) throws SQLException {
        String sql = "SELECT value FROM app_state WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("value")) : Optional.empty();
            }
        }
    }

    public Optional<Integer> getInt(String key) throws SQLException {
        return get(key).map(Integer::parseInt);
    }

    public void set(String key, String value) throws SQLException {
        String sql = "INSERT OR REPLACE INTO app_state(key, value) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    public void setInt(String key, int value) throws SQLException {
        set(key, String.valueOf(value));
    }

}