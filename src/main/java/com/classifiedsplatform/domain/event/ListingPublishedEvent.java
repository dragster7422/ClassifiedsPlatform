package com.classifiedsplatform.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class ListingPublishedEvent {
    private final UUID listingId;
    private final String title;
    private final LocalDateTime publishedAt;

    public ListingPublishedEvent(UUID listingId, String title, LocalDateTime publishedAt) {
        this.listingId = listingId;
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}