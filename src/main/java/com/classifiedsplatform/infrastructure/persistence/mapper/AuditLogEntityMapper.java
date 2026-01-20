package com.classifiedsplatform.infrastructure.persistence.mapper;

import com.classifiedsplatform.domain.model.AuditLog;
import com.classifiedsplatform.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditLogEntityMapper {

    public AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }

        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(domain.getId());
        entity.setEventType(domain.getEventType());
        entity.setListingId(domain.getListingId());
        entity.setPayloadJson(domain.getPayloadJson());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }

        // Use reconstitute factory method for persistence layer
        return AuditLog.reconstitute(
                entity.getId(),
                entity.getEventType(),
                entity.getListingId(),
                entity.getPayloadJson(),
                entity.getCreatedAt()
        );
    }
}