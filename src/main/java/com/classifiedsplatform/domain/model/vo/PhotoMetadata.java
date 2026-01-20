package com.classifiedsplatform.domain.model.vo;

import com.classifiedsplatform.domain.exception.InvalidListingPhotoFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PhotoMetadata {
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

    private final String filename;
    private final String contentType;
    private final long size;

    private PhotoMetadata(String filename, String contentType, long size) {
        validateFilename(filename);
        validateContentType(contentType);
        validateSize(size);

        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
    }

    public static PhotoMetadata of(String filename, String contentType, long size) {
        return new PhotoMetadata(filename, contentType, size);
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidListingPhotoFormatException(
                    "Invalid content type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }
    }

    private void validateSize(long size) {
        if (size <= 0) {
            throw new InvalidListingPhotoFormatException("File size must be positive");
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new InvalidListingPhotoFormatException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + "MB"
            );
        }
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoMetadata that = (PhotoMetadata) o;
        return size == that.size &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, contentType, size);
    }

    @Override
    public String toString() {
        return "PhotoMetadata{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                '}';
    }
}