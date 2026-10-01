package com.rentaltracker.repository;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import jdk.jfr.Frequency;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            statement.setInt(1, item.getOwnerId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setInt(4, item.getCostPerDay());
            statement.setString(5, item.getStatus().name());
            statement.setObject(6, item.getCreatedAt());

            statement.executeUpdate();

            try(ResultSet generatedKeys = statement.getGeneratedKeys()){
                if (generatedKeys.next()){
                    item.setItemId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Optional<ItemDomain> findByItemId (int itemId) throws SQLException {
        String sql = "SELECT item_id, owner_id, item_name, description, cost_per_day, status, created_at FROM listed_items WHERE item_id = ?";

        //Give the where id = ? value
        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, itemId);

            try (ResultSet resultSet = statement.executeQuery()){
                if (resultSet.next()){
                    return Optional.of(mapResultSetToItemDomain(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public List<ItemDomain> findAll() throws SQLException {
        String sql = "SELECT item_id, owner_id, item_name, description, cost_per_day, status, created_at FROM listed_items";
        List<ItemDomain> listedItems= new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()){
            while (resultSet.next()){
                listedItems.add(mapResultSetToItemDomain(resultSet));
            }
        }
        return listedItems;
    }

    public void update(ItemDomain item) throws SQLException {
        String sql = "UPDATE users SET owner_id = ?, item_name = ?, description = ?, cost_per_day = ?, status = ? WHERE item_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, item.getOwnerId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setInt(4, item.getCostPerDay());
            statement.setString(5, item.getStatus().name());

            statement.executeUpdate();
        }
    }

    public void deleteById (int itemId) throws SQLException {
        String sql = "DELETE FROM listed_items WHERE id = ?";

        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            statement.executeUpdate();
        }
    }

    private ItemDomain mapResultSetToItemDomain(ResultSet resultSet) throws SQLException {
        String status = resultSet.getString("status");
        ItemStatus itemStatus = ItemStatus.safeValueOf(status);

        LocalDateTime createdAt = resultSet.getObject("created_at", LocalDateTime.class);

        return new ItemDomain(
                resultSet.getInt("item_id"),
                resultSet.getInt("owner_id"),
                resultSet.getString("item_name"),
                resultSet.getString("description"),
                resultSet.getInt("cost_per_day"),
                itemStatus,
                createdAt
        );
    }


}
