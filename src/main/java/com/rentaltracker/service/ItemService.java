package com.rentaltracker.service;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.repository.ItemRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    //CREATE

    public ItemDomain createItem(int ownerId, String itemName, String description, int costPerDay) throws SQLException {
        return createItem(ownerId, itemName, description, costPerDay, ItemStatus.available);
    }

    public ItemDomain createItem(int ownerId, String itemName, String description, int costPerDay, ItemStatus status) throws SQLException {
        validateOwnerId(ownerId);
        validateItemName(itemName);
        validateCostPerDay(costPerDay);
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        ItemDomain item = new ItemDomain();
        item.setOwnerId(ownerId);
        item.setItemName(itemName.trim());
        item.setDescription(description == null ? "" : description.trim());
        item.setCostPerDay(costPerDay);
        item.setStatus(status);
        item.setCreatedAt(LocalDateTime.now());

        itemRepository.save(item);
        return item;
    }

    //READ
    public Optional<ItemDomain> getItemById(int itemId) throws SQLException {
        validateItemId(itemId);
        return itemRepository.findByItemId(itemId);
    }

    public ItemDomain requireItemById(int itemId) throws SQLException {
        return getItemById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
    }

    public List<ItemDomain> getAllItems() throws SQLException {
        return itemRepository.findAll();
    }

    public List<ItemDomain> getItemsByOwner(int ownerId) throws SQLException {
        validateOwnerId(ownerId);
        return itemRepository.findAll().stream()
                .filter(i -> i.getOwnerId() == ownerId)
                .collect(Collectors.toList());
    }

    public List<ItemDomain> getItemsByStatus(ItemStatus status) throws SQLException {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return itemRepository.findAll().stream()
                .filter(i -> i.getStatus() == status)
                .collect(Collectors.toList());
    }

    //UPDATE
    public ItemDomain updateItem(int itemId, String itemName, String description, int costPerDay, ItemStatus status) throws SQLException {
        validateItemId(itemId);
        validateItemName(itemName);
        validateCostPerDay(costPerDay);
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        ItemDomain existing = requireItemById(itemId);
        existing.setItemName(itemName.trim());
        existing.setDescription(description == null ? "" : description.trim());
        existing.setCostPerDay(costPerDay);
        existing.setStatus(status);

        itemRepository.update(existing);
        return existing;
    }

    public ItemDomain changeStatus(int itemId, ItemStatus newStatus) throws SQLException {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        ItemDomain existing = requireItemById(itemId);
        existing.setStatus(newStatus);
        itemRepository.update(existing);
        return existing;
    }

    //DELETE
    public boolean deleteItem(int itemId) throws SQLException {
        validateItemId(itemId);
        if (itemRepository.findByItemId(itemId).isEmpty()) {
            return false;
        }
        itemRepository.deleteById(itemId);
        return true;
    }

    //VALIDATION HELPERS

    private void validateItemId(int itemId) {
        if (itemId <= 0) {
            throw new IllegalArgumentException("itemId must be positive");
        }
    }
    private void validateOwnerId(int ownerId) {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("ownerId must be positive");
        }
    }

    private void validateItemName(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }
        if (itemName.trim().length() > 100) {
            throw new IllegalArgumentException("Item name too long (max 100 chars)");
        }
    }

    private void validateCostPerDay(int costPerDay) {
        if (costPerDay < 0) {
            throw new IllegalArgumentException("costPerDay cannot be negative");
        }
    }
}