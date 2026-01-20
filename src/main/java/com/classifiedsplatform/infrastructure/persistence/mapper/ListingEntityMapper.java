package com.classifiedsplatform.infrastructure.persistence.mapper;

import com.classifiedsplatform.domain.model.Listing;
import com.classifiedsplatform.domain.model.ListingPhoto;
import com.classifiedsplatform.domain.model.vo.Money;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingEntity;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingPhotoEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ListingEntityMapper {

    private final PhotoEntityMapper photoMapper;

    public ListingEntityMapper(PhotoEntityMapper photoMapper) {
        this.photoMapper = photoMapper;
    }

    public ListingEntity toEntity(Listing domain) {
        if (domain == null) {
            return null;
        }

        ListingEntity entity = new ListingEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setPriceAmount(domain.getPrice().getAmount());
        entity.setPriceCurrency(domain.getPrice().getCurrency());
        entity.setCategory(domain.getCategory());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());

        // Map photos
        List<ListingPhotoEntity> photoEntities = new ArrayList<>();
        for (ListingPhoto photo : domain.getPhotos()) {
            ListingPhotoEntity photoEntity = photoMapper.toEntity(photo);
            photoEntity.setListing(entity);
            photoEntities.add(photoEntity);
        }
        entity.setPhotos(photoEntities);

        return entity;
    }

    public Listing toDomain(ListingEntity entity) {
        if (entity == null) {
            return null;
        }

        // Map photos
        List<ListingPhoto> photos = new ArrayList<>();
        for (ListingPhotoEntity photoEntity : entity.getPhotos()) {
            photos.add(photoMapper.toDomain(photoEntity));
        }

        // Use reconstitute factory method for persistence layer
        return Listing.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                Money.of(entity.getPriceAmount(), entity.getPriceCurrency()),
                entity.getCategory(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                photos
        );
    }

    public void updateEntity(Listing domain, ListingEntity entity) {
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setPriceAmount(domain.getPrice().getAmount());
        entity.setPriceCurrency(domain.getPrice().getCurrency());
        entity.setCategory(domain.getCategory());
        entity.setStatus(domain.getStatus());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }
}