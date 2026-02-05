package com.smartretail.backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "shops")
public class Shop {
    @Id
    private String id;
    private String name;
    private String ownerId; // Link to the user who owns the shop
    private Date createdAt;

    public Shop() {
    }

    public Shop(String name, String ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = new Date();
    }

    // Manual getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
