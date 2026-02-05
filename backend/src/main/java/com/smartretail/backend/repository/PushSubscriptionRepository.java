package com.smartretail.backend.repository;

import com.smartretail.backend.models.PushSubscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PushSubscriptionRepository extends MongoRepository<PushSubscription, String> {
    Optional<PushSubscription> findByUserIdAndShopId(String userId, String shopId);
    java.util.List<PushSubscription> findByShopId(String shopId);
}