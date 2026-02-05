package com.smartretail.backend.config;

import lombok.Getter;

// InsufficientStockException.java
@Getter
public class InsufficientStockException extends RuntimeException {
    private final String productId;

    public InsufficientStockException(String productId) {
        super("Insufficient stock for product: " + productId);
        this.productId = productId;
    }

}
