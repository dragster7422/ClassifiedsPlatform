package com.classifiedsplatform.domain.exception;

import java.util.UUID;

public class IdempotencyConflictException extends DomainException {

    private final String idempotencyKey;
    private final UUID listingId;

    public IdempotencyConflictException(String idempotencyKey, UUID listingId) {
        super(String.format("Operation with idempotency key '%s' already processed for listing %s",
                idempotencyKey, listingId));
        this.idempotencyKey = idempotencyKey;
        this.listingId = listingId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getListingId() {
        return listingId;
    }
}