package com.smartretail.backend.service;

import com.smartretail.backend.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.Map;

public interface AuditLogService {
    void logAction(String actionType, String entityId, String userEmail, Map<String, Object> details);
    Page<AuditLog> getAuditLogs(String actionType, Date startDate, Date endDate, Pageable pageable);
}