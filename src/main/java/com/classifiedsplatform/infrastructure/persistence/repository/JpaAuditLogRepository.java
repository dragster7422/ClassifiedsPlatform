package com.classifiedsplatform.infrastructure.persistence.repository;

import com.classifiedsplatform.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    List<AuditLogEntity> findByEventTypeOrderByCreatedAtDesc(String eventType);
}