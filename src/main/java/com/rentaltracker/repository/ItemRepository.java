package com.rentaltracker.repository;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.infrastructure.DbTime;
import com.rentaltracker.repository.exception.MappingException;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

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

    public void save(ItemDomain item)  {
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
            statement.setString(6, DbTime.format(item.getCreatedAt()));

            statement.executeUpdate();

            try(ResultSet generatedKeys = statement.getGeneratedKeys()){
                if (generatedKeys.next()){
                    item.setItemId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("insert item", e);
        }
    }

    public Optional<ItemDomain> findByItemId (int itemId)  {
        String sql = "SELECT item_id, owner_id, item_name, description, cost_per_day, status, created_at FROM listed_items WHERE item_id = ?";

        //Give the where id = ? value
        try (PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, itemId);

            try (ResultSet resultSet = statement.executeQuery()){
                if (resultSet.next()){
                    return Optional.of(mapResultSetToItemDomain(resultSet));
                }
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find item by id", e);
        }
        return Optional.empty();
    }

    public List<ItemDomain> findAll() {
        String sql = "SELECT item_id, owner_id, item_name, description, cost_per_day, status, created_at FROM listed_items";
        List<ItemDomain> listedItems= new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()){
            while (resultSet.next()){
                listedItems.add(mapResultSetToItemDomain(resultSet));
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find all items", e);
        }
        return listedItems;
    }

    public void update(ItemDomain item)  {
        String sql = "UPDATE listed_items SET owner_id = ?, item_name = ?, description = ?, cost_per_day = ?, status = ? WHERE item_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, item.getOwnerId());
            statement.setString(2, item.getItemName());
            statement.setString(3, item.getDescription());
            statement.setInt(4, item.getCostPerDay());
            statement.setString(5, item.getStatus().name());
            statement.setInt(6, item.getItemId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("update existing item", e);
        }
    }

    public void deleteById (int itemId)  {
        String sql = "DELETE FROM listed_items WHERE item_id = ?";

        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, itemId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("delete item by id", e);
        }
    }

    private ItemDomain mapResultSetToItemDomain(ResultSet resultSet) {
        try {
            return new ItemDomain(
                    resultSet.getInt("item_id"),
                    resultSet.getInt("owner_id"),
                    resultSet.getString("item_name"),
                    resultSet.getString("description"),
                    resultSet.getInt("cost_per_day"),
                    ItemStatus.fromDb(resultSet.getString("status")),
                    DbTime.parse(resultSet.getString("created_at")));
        } catch (SQLException | RuntimeException e) {
            throw new MappingException("Cannot map item row", e);
        }
    }


}
