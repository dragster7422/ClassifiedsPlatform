package com.classifiedsplatform.domain.exception;

import java.util.UUID;

public class ListingNotFoundException extends DomainException {
    public ListingNotFoundException(UUID listingId) {
        super(String.format("Listing with id %s not found", listingId));
    }
}