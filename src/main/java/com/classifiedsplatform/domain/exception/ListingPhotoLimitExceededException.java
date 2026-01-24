package com.classifiedsplatform.domain.exception;

public class ListingPhotoLimitExceededException extends DomainException {

    // Original constructor for single photo upload
    public ListingPhotoLimitExceededException(int maxPhotos) {
        super(String.format("Maximum number of photos (%d) exceeded", maxPhotos));
    }

    // Constructor for batch photo upload with detailed information
    public ListingPhotoLimitExceededException(int maxPhotos, int currentCount, int attemptedToAdd) {
        super(String.format(
                "Cannot add %d photo(s). Listing already has %d photo(s) and maximum allowed is %d",
                attemptedToAdd,
                currentCount,
                maxPhotos
        ));
    }
}