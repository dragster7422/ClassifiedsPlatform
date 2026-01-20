package com.classifiedsplatform.infrastructure.persistence.mapper;

import com.classifiedsplatform.domain.model.IdempotencyRecord;
import com.classifiedsplatform.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyRecordEntityMapper {

    public IdempotencyRecordEntity toEntity(IdempotencyRecord domain) {
        if (domain == null) {
            return null;
        }

        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setId(domain.getId());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setListingId(domain.getListingId());
        entity.setResultJson(domain.getResultJson());
        entity.setHttpStatus(domain.getHttpStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiresAt(domain.getExpiresAt());

        return entity;
    }

    public IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        // Use reconstitute factory method for persistence layer
        return IdempotencyRecord.reconstitute(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getListingId(),
                entity.getResultJson(),
                entity.getHttpStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}