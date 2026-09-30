package com.rentaltracker.repository;

import com.rentaltracker.domain.ItemDomain;

import java.sql.*;
import java.time.LocalDateTime;

public class ItemRepository {
    private final Connection connection;

    public ItemRepository (Connection connection){
        this.connection = connection;
    }

    public void save(ItemDomain item) throws SQLException{
        String sql = "INSERT INTO listed_items (owner_id, item_name, description, cost_per_day, status, created_at) " +
                "VALUES (?,?,?,?,?,?)";

        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);

        try(PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            statement.setInt(2, item.getOwnerId());
            statement.setString(3, item.getItemName());
            statement.setString(4, item.getDescription());
            statement.setInt(5, item.getCostPerDay());
            statement.setString(6, item.getStatus().name());
            statement.setObject(7, item.getCreatedAt());

            statement.executeUpdate();

            try(ResultSet generatedKeys = statement.getGeneratedKeys()){
                if (generatedKeys.next()){
                    item.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
}
