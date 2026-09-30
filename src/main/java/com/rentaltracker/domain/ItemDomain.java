package com.rentaltracker.domain;

import com.rentaltracker.domain.enums.ItemStatus;
import java.time.LocalDateTime;

public class ItemDomain{
    private int id;
    private int ownerId;
    private String itemName;
    private String description;
    private int costPerDay;
    private ItemStatus status;
    private LocalDateTime createdAt;

    public ItemDomain (){

    }
    public ItemDomain(int id, int ownerId, String itemName, String description,
                      int costPerDay, ItemStatus status, LocalDateTime createdAt){
        this.id = id;
        this.ownerId = ownerId;
        this.itemName = itemName;
        this.description = description;
        this.costPerDay = costPerDay;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public int getCostPerDay() {
        return costPerDay;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCostPerDay(int costPerDay) {
        this.costPerDay = costPerDay;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}