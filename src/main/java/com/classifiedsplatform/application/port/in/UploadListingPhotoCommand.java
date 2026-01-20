package com.classifiedsplatform.application.port.in;

import java.util.UUID;

public record UploadListingPhotoCommand(
        UUID listingId,
        String filename,
        String contentType,
        long fileSize,
        byte[] fileData
) {
    public UploadListingPhotoCommand {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty");
        }
    }
}