package com.smartretail.backend.service;

import com.smartretail.backend.models.Product;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public interface ProductService {

    boolean existsByProductId(String productId);

    Product createProduct(Product product, MultipartFile imageFile, Locale locale) throws IOException;
    Product createProduct(Product product, Locale locale);                     // no image

    Product getProductById(String productId, Locale locale);
    List<Product> getAllProducts();

    Product updateProduct(String productId, Product product, MultipartFile imageFile, Locale locale) throws IOException;
    Product updateProduct(String productId, Product product, Locale locale);   // no image
    Product updateProduct(String productId, Product product, Locale locale, boolean isSyncMode);

    void deleteProduct(String productId, Locale locale);

    List<Product> getLowStockProducts();
    List<Product> getExpiringProducts(Date threshold);

    Product restockProduct(String productId, int restockQty, Locale locale);
    Product updateProductQuantity(String productId, int quantity, Locale locale);
}