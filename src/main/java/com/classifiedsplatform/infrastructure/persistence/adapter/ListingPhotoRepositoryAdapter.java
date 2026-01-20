package com.classifiedsplatform.infrastructure.persistence.adapter;

import com.classifiedsplatform.application.port.out.ListingPhotoRepository;
import com.classifiedsplatform.domain.model.ListingPhoto;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingPhotoEntity;
import com.classifiedsplatform.infrastructure.persistence.mapper.PhotoEntityMapper;
import com.classifiedsplatform.infrastructure.persistence.repository.JpaListingPhotoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional
public class ListingPhotoRepositoryAdapter implements ListingPhotoRepository {

    private final JpaListingPhotoRepository jpaRepository;
    private final PhotoEntityMapper mapper;

    public ListingPhotoRepositoryAdapter(JpaListingPhotoRepository jpaRepository, PhotoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ListingPhoto save(ListingPhoto photo) {
        ListingPhotoEntity entity = mapper.toEntity(photo);
        ListingPhotoEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingPhoto> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingPhoto> findByListingId(UUID listingId) {
        return jpaRepository.findByListingId(listingId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByListingId(UUID listingId) {
        return jpaRepository.countByListingId(listingId);
    }

    @Override
    public void delete(ListingPhoto photo) {
        jpaRepository.deleteById(photo.getId());
    }
}