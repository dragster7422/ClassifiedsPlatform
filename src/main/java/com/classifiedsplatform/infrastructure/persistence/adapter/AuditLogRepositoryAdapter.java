package com.classifiedsplatform.infrastructure.persistence.adapter;

import com.classifiedsplatform.application.port.out.AuditLogRepository;
import com.classifiedsplatform.domain.model.AuditLog;
import com.classifiedsplatform.infrastructure.persistence.entity.AuditLogEntity;
import com.classifiedsplatform.infrastructure.persistence.mapper.AuditLogEntityMapper;
import com.classifiedsplatform.infrastructure.persistence.repository.JpaAuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;
    private final AuditLogEntityMapper mapper;

    public AuditLogRepositoryAdapter(JpaAuditLogRepository jpaRepository, AuditLogEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = mapper.toEntity(auditLog);
        AuditLogEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByListingId(UUID listingId) {
        return jpaRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByEventType(String eventType) {
        return jpaRepository.findByEventTypeOrderByCreatedAtDesc(eventType).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}