package com.smartretail.backend.repository;

import com.smartretail.backend.models.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByPaymentIdAndShopId(String paymentId, String shopId);
    boolean existsByPaymentIdAndShopId(String paymentId, String shopId);
    java.util.List<Payment> findByShopId(String shopId);
}