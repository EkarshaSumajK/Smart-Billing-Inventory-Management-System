package com.smartretail.backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Document(collection = "payments")
public class Payment {
    @Id
    private String id; // Maps to _id
    @Indexed(unique = true)
    private String paymentId; // e.g., "pay_2508171011"
    @Field("billId")
    private String billId; // References bills._id
    private double amount;
    private String paymentMode; // "CASH", "UPI"
    private String status; // "INITIATED", "SUCCESS", "FAILED"
    private String upiTransactionId; // Nullable
    private String shopId;
    private Date createdAt;

    public Payment() {
    }

    public Payment(String paymentId, String billId, double amount, String paymentMode, String status, String shopId) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.status = status;
        this.shopId = shopId;
        this.createdAt = new Date();
    }

    // Manual getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpiTransactionId() {
        return upiTransactionId;
    }

    public void setUpiTransactionId(String upiTransactionId) {
        this.upiTransactionId = upiTransactionId;
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
}