package com.classifiedsplatform.application.port.out;

import com.classifiedsplatform.domain.model.ListingPhoto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingPhotoRepository {

    ListingPhoto save(ListingPhoto photo);

    Optional<ListingPhoto> findById(UUID id);

    List<ListingPhoto> findByListingId(UUID listingId);

    long countByListingId(UUID listingId);

    void delete(ListingPhoto photo);
}