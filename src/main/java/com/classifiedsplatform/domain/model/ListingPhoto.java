package com.classifiedsplatform.domain.model;

import com.classifiedsplatform.domain.model.vo.PhotoMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

public class ListingPhoto {
    private UUID id;
    private UUID listingId;
    private PhotoMetadata metadata;
    private String storagePath;
    private LocalDateTime createdAt;

    private ListingPhoto() {}

    // Factory method for creating NEW photos (business logic)
    public static ListingPhoto create(UUID listingId, PhotoMetadata metadata, String storagePath) {
        validateListingId(listingId);
        validateMetadata(metadata);
        validateStoragePath(storagePath);

        ListingPhoto photo = new ListingPhoto();
        photo.id = UUID.randomUUID();
        photo.listingId = listingId;
        photo.metadata = metadata;
        photo.storagePath = storagePath;
        photo.createdAt = LocalDateTime.now();
        return photo;
    }

    // Factory method for RECONSTITUTING from persistence layer
    public static ListingPhoto reconstitute(
            UUID id,
            UUID listingId,
            PhotoMetadata metadata,
            String storagePath,
            LocalDateTime createdAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null when reconstituting");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null when reconstituting");
        }

        validateListingId(listingId);
        validateMetadata(metadata);
        validateStoragePath(storagePath);

        ListingPhoto photo = new ListingPhoto();
        photo.id = id;
        photo.listingId = listingId;
        photo.metadata = metadata;
        photo.storagePath = storagePath;
        photo.createdAt = createdAt;
        return photo;
    }

    // Validation methods
    private static void validateListingId(UUID listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
    }

    private static void validateMetadata(PhotoMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Photo metadata cannot be null");
        }
    }

    private static void validateStoragePath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getListingId() {
        return listingId;
    }

    public PhotoMetadata getMetadata() {
        return metadata;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}