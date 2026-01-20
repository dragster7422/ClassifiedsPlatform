package com.classifiedsplatform.application.service;

import com.classifiedsplatform.application.port.out.AuditLogRepository;
import com.classifiedsplatform.domain.event.ListingPublishedEvent;
import com.classifiedsplatform.domain.event.PhotoUploadedEvent;
import com.classifiedsplatform.domain.model.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final String EVENT_LISTING_PUBLISHED = "LISTING_PUBLISHED";
    private static final String EVENT_PHOTO_UPLOADED = "PHOTO_UPLOADED";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logListingPublished(ListingPublishedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            AuditLog auditLog = AuditLog.create(
                    EVENT_LISTING_PUBLISHED,
                    event.getListingId(),
                    payload
            );
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for listing published: {}", event.getListingId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON", e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPhotoUploaded(PhotoUploadedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            AuditLog auditLog = AuditLog.create(
                    EVENT_PHOTO_UPLOADED,
                    event.getListingId(),
                    payload
            );
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for photo uploaded: {}", event.getPhotoId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON", e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }
}