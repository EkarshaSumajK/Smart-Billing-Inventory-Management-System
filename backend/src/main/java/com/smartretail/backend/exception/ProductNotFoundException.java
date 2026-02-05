package com.smartretail.backend.exception;

public class ProductNotFoundException extends RuntimeException {
    private final String productId;

    public ProductNotFoundException(String productId) {
        super(productId);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
