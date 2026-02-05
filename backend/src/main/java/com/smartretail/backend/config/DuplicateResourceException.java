package com.smartretail.backend.config;

import lombok.Getter;

// DuplicateResourceException.java
@Getter
public class DuplicateResourceException extends RuntimeException {
    private final String messageKey;
    private final String resourceId;

    public DuplicateResourceException(String messageKey, String resourceId) {
        super("Duplicate resource: " + resourceId);
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
