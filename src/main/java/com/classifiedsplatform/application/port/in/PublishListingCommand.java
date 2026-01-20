package com.classifiedsplatform.application.port.in;

import java.util.UUID;

public record PublishListingCommand(
        UUID listingId,
        String idempotencyKey
) {
    public PublishListingCommand {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
    }
}