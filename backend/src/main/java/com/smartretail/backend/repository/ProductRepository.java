package com.smartretail.backend.repository;

import com.smartretail.backend.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByProductIdAndShopId(String productId, String shopId);
    boolean existsByProductIdAndShopId(String productId, String shopId);
    List<Product> findByShopIdAndExpiryDateBefore(String shopId, Date threshold);
    
    @Query("{ 'shopId': ?0, $expr: { $lt: ['$quantity', '$reorderLevel'] } }")
    List<Product> findLowStockProducts(String shopId);
    
    @Query(value = "{ 'productId' : ?0, 'shopId': ?1 }", delete = true)
    void deleteByProductIdAndShopId(String productId, String shopId);
    
    List<Product> findByShopId(String shopId);
}