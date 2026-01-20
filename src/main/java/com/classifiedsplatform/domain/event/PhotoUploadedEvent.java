package com.classifiedsplatform.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class PhotoUploadedEvent {
    private final UUID photoId;
    private final UUID listingId;
    private final String filename;
    private final long fileSize;
    private final LocalDateTime uploadedAt;

    public PhotoUploadedEvent(UUID photoId, UUID listingId, String filename, long fileSize, LocalDateTime uploadedAt) {
        this.photoId = photoId;
        this.listingId = listingId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public UUID getPhotoId() {
        return photoId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}