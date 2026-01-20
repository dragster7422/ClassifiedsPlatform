package com.classifiedsplatform.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLog {
    private UUID id;
    private String eventType;
    private UUID listingId;
    private String payloadJson;
    private LocalDateTime createdAt;

    private AuditLog() {}

    // Factory method for creating NEW audit logs (business logic)
    public static AuditLog create(String eventType, UUID listingId, String payloadJson) {
        validateEventType(eventType);
        validateListingId(listingId);
        validatePayload(payloadJson);

        AuditLog auditLog = new AuditLog();
        auditLog.id = UUID.randomUUID();
        auditLog.eventType = eventType;
        auditLog.listingId = listingId;
        auditLog.payloadJson = payloadJson;
        auditLog.createdAt = LocalDateTime.now();
        return auditLog;
    }

    // Factory method for RECONSTITUTING from persistence layer
    public static AuditLog reconstitute(
            UUID id,
            String eventType,
            UUID listingId,
            String payloadJson,
            LocalDateTime createdAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null when reconstituting");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null when reconstituting");
        }

        validateEventType(eventType);
        validateListingId(listingId);
        validatePayload(payloadJson);

        AuditLog auditLog = new AuditLog();
        auditLog.id = id;
        auditLog.eventType = eventType;
        auditLog.listingId = listingId;
        auditLog.payloadJson = payloadJson;
        auditLog.createdAt = createdAt;
        return auditLog;
    }

    // Validation methods
    private static void validateEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
    }

    private static void validateListingId(UUID listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
    }

    private static void validatePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}