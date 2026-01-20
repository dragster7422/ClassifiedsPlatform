package com.classifiedsplatform.domain.exception;

public class ListingPhotoLimitExceededException extends DomainException {
    public ListingPhotoLimitExceededException(int maxPhotos) {
        super(String.format("Maximum number of photos (%d) exceeded", maxPhotos));
    }
}