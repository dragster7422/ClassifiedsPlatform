package com.classifiedsplatform.infrastructure.persistence.repository;

import com.classifiedsplatform.infrastructure.persistence.entity.ListingPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaListingPhotoRepository extends JpaRepository<ListingPhotoEntity, UUID> {

    @Query("SELECT p FROM ListingPhotoEntity p WHERE p.listing.id = :listingId")
    List<ListingPhotoEntity> findByListingId(@Param("listingId") UUID listingId);

    @Query("SELECT COUNT(p) FROM ListingPhotoEntity p WHERE p.listing.id = :listingId")
    long countByListingId(@Param("listingId") UUID listingId);
}