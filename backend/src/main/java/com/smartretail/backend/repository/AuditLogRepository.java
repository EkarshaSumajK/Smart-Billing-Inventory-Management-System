package com.smartretail.backend.repository;

import com.smartretail.backend.models.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    java.util.List<AuditLog> findByShopId(String shopId);
}