package com.smartretail.backend.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection = "products")
public class Product {

    @Id
    private String id; // MongoDB _id

    @NotBlank(message = "Product ID is required")
    @Indexed(unique = true)
    private String productId; // custom business ID

    @NotBlank(message = "Product name is required")
    private String name;

    private String category;
    private double price;
    private int quantity;
    private int minQuantity; // minimum stock before alert
    private int reorderLevel;
    private Date expiryDate;
    private String imageId; // GridFS file id
    private String imageUrl; // public URL
    private String supplierEmail;
    private String addedBy;
    private Date lastUpdated;
    private String shopId;
    private Date createdAt;

    // Manual getters/setters to bypass Lombok issues
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity) {
        this.minQuantity = minQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSupplierEmail() {
        return supplierEmail;
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
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

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Product() {
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    public Product(String productId, String name, String category, double price, int quantity,
            int minQuantity, int reorderLevel, Date expiryDate, String imageId,
            String imageUrl, String supplierEmail, String addedBy, String shopId) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
        this.reorderLevel = reorderLevel;
        this.expiryDate = expiryDate;
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.supplierEmail = supplierEmail;
        this.addedBy = addedBy;
        this.shopId = shopId;
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }
}