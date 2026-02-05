package com.smartretail.backend.config;

import lombok.Getter;

// ResourceNotFoundException.java
@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String messageKey;
    private final String resourceId;

    public ResourceNotFoundException(String messageKey, String resourceId) {
        super("Resource not found: " + resourceId);
        this.messageKey = messageKey;
        this.resourceId = resourceId;
    }

    // Manual getters
    public String getMessageKey() {
        return messageKey;
    }

    public String getResourceId() {
        return resourceId;
    }
}
