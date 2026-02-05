package com.smartretail.backend.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
@Document(collection = "customers")
public class Customer {
    // Getters and Setters
    @Id
    private String id;
    private String name;
    private String email;
    private String mobile;
    private String shopId;
    private Date createdAt;
    private List<String> purchaseHistory;
    private int totalPurchaseCount;
    private Date lastPurchaseDate;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getPurchaseHistory() {
        return purchaseHistory;
    }

    public void setPurchaseHistory(List<String> purchaseHistory) {
        this.purchaseHistory = purchaseHistory;
    }

    public int getTotalPurchaseCount() {
        return totalPurchaseCount;
    }

    public void setTotalPurchaseCount(int totalPurchaseCount) {
        this.totalPurchaseCount = totalPurchaseCount;
    }

    public Date getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(Date lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public Customer() {
        this.purchaseHistory = new ArrayList<>();
        this.createdAt = new Date();
        this.totalPurchaseCount = 0;
        this.lastPurchaseDate = null;
    }

    public Customer(String name, String email, String mobile, String shopId) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.shopId = shopId;
        this.createdAt = new Date();
        this.purchaseHistory = new ArrayList<>();
        this.totalPurchaseCount = 0;
        this.lastPurchaseDate = null;
    }

    public void addBillId(String billId) {
        if (this.purchaseHistory == null) {
            this.purchaseHistory = new ArrayList<>();
        }
        this.purchaseHistory.add(billId);
    }
}