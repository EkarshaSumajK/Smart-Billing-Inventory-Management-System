package com.smartretail.backend.repository;

import com.smartretail.backend.models.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    java.util.List<Notification> findByShopId(String shopId);
}