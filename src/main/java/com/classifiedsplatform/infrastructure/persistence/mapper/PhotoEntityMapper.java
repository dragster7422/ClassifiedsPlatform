package com.classifiedsplatform.infrastructure.persistence.mapper;

import com.classifiedsplatform.domain.model.ListingPhoto;
import com.classifiedsplatform.domain.model.vo.PhotoMetadata;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingEntity;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingPhotoEntity;
import com.classifiedsplatform.infrastructure.persistence.repository.JpaListingRepository;
import org.springframework.stereotype.Component;

@Component
public class PhotoEntityMapper {

    private final JpaListingRepository listingRepository;

    public PhotoEntityMapper(JpaListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public ListingPhotoEntity toEntity(ListingPhoto domain) {
        if (domain == null) {
            return null;
        }

        ListingPhotoEntity entity = new ListingPhotoEntity();
        entity.setId(domain.getId());

        ListingEntity listing = listingRepository.findById(domain.getListingId())
                .orElseThrow(() -> new IllegalStateException(
                        "Listing not found: " + domain.getListingId()
                ));
        entity.setListing(listing);

        entity.setFilename(domain.getMetadata().getFilename());
        entity.setContentType(domain.getMetadata().getContentType());
        entity.setFileSize(domain.getMetadata().getSize());
        entity.setStoragePath(domain.getStoragePath());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    public ListingPhoto toDomain(ListingPhotoEntity entity) {
        if (entity == null) {
            return null;
        }

        PhotoMetadata metadata = PhotoMetadata.of(
                entity.getFilename(),
                entity.getContentType(),
                entity.getFileSize()
        );

        // Use reconstitute factory method for persistence layer
        return ListingPhoto.reconstitute(
                entity.getId(),
                entity.getListingId(),
                metadata,
                entity.getStoragePath(),
                entity.getCreatedAt()
        );
    }
}