package com.classifiedsplatform.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdempotencyRecord {
    private static final int EXPIRATION_HOURS = 24;

    private UUID id;
    private String idempotencyKey;
    private UUID listingId;
    private String resultJson;
    private int httpStatus;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private IdempotencyRecord() {}

    // Factory method for creating NEW idempotency records (business logic)
    public static IdempotencyRecord create(String idempotencyKey, UUID listingId, String resultJson, int httpStatus) {
        validateIdempotencyKey(idempotencyKey);
        validateListingId(listingId);
        validateResultJson(resultJson);
        validateHttpStatus(httpStatus);

        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.idempotencyKey = idempotencyKey;
        record.listingId = listingId;
        record.resultJson = resultJson;
        record.httpStatus = httpStatus;
        record.createdAt = LocalDateTime.now();
        record.expiresAt = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
        return record;
    }

    // Factory method for RECONSTITUTING from persistence layer
    public static IdempotencyRecord reconstitute(
            UUID id,
            String idempotencyKey,
            UUID listingId,
            String resultJson,
            int httpStatus,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null when reconstituting");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null when reconstituting");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("ExpiresAt cannot be null when reconstituting");
        }

        validateIdempotencyKey(idempotencyKey);
        validateListingId(listingId);
        validateResultJson(resultJson);
        validateHttpStatus(httpStatus);

        IdempotencyRecord record = new IdempotencyRecord();
        record.id = id;
        record.idempotencyKey = idempotencyKey;
        record.listingId = listingId;
        record.resultJson = resultJson;
        record.httpStatus = httpStatus;
        record.createdAt = createdAt;
        record.expiresAt = expiresAt;
        return record;
    }

    // Validation methods
    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
    }

    private static void validateListingId(UUID listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
    }

    private static void validateResultJson(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            throw new IllegalArgumentException("Result JSON cannot be null or empty");
        }
    }

    private static void validateHttpStatus(int httpStatus) {
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("HTTP status must be between 100 and 599");
        }
    }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getResultJson() {
        return resultJson;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}