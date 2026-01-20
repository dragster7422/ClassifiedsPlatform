package com.classifiedsplatform.application.port.out;

import com.classifiedsplatform.domain.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByListingId(UUID listingId);

    List<AuditLog> findByEventType(String eventType);
}