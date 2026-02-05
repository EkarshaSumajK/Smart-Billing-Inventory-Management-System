package com.smartretail.backend.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Setter
@Getter
@Document(collection = "auditLogs")
public class AuditLog {
    // Getters and Setters
    @Id
    private String id;
    private String actionType;
    private String entityId;
    private String userEmail;
    private String shopId;
    private Date timestamp;
    private Map<String, Object> details;

    // Constructors
    public AuditLog() {
    }

    public AuditLog(String actionType, String entityId, String userEmail, String shopId, Date timestamp,
            Map<String, Object> details) {
        this.actionType = actionType;
        this.entityId = entityId;
        this.userEmail = userEmail;
        this.shopId = shopId;
        this.timestamp = timestamp;
        this.details = details;
    }

    // Manual getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}