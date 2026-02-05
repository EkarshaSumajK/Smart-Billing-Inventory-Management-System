package com.smartretail.backend.service;

import com.smartretail.backend.models.AuditLog;
import com.smartretail.backend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;
    private final com.smartretail.backend.security.SecurityUtils securityUtils;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, MongoTemplate mongoTemplate,
            com.smartretail.backend.security.SecurityUtils securityUtils) {
        this.auditLogRepository = auditLogRepository;
        this.mongoTemplate = mongoTemplate;
        this.securityUtils = securityUtils;
    }

    @Override
    public void logAction(String actionType, String entityId, String userEmail, Map<String, Object> details) {
        logger.debug("[SERVICE] Logging action: type={}, entityId={}, userEmail={}", actionType, entityId, userEmail);
        String shopId = securityUtils.getCurrentShopId();
        AuditLog auditLog = new AuditLog(actionType, entityId, userEmail, shopId, new Date(), details);
        auditLogRepository.save(auditLog);
        logger.info("[SERVICE] Audit log saved: type={}, entityId={}", actionType, entityId);
    }

    @Override
    public Page<AuditLog> getAuditLogs(String actionType, Date startDate, Date endDate, Pageable pageable) {
        logger.debug("[SERVICE] Fetching audit logs: actionType={}, startDate={}, endDate={}, page={}, size={}",
                actionType, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        Query query = new Query();
        String shopId = securityUtils.getCurrentShopId();
        query.addCriteria(Criteria.where("shopId").is(shopId));
        if (actionType != null && !actionType.isEmpty()) {
            query.addCriteria(Criteria.where("actionType").is(actionType));
        }
        if (startDate != null && endDate != null) {
            query.addCriteria(Criteria.where("timestamp").gte(startDate).lte(endDate));
        }
        query.with(pageable);

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AuditLog.class);
        return PageableExecutionUtils.getPage(mongoTemplate.find(query, AuditLog.class), pageable, () -> total);
    }
}