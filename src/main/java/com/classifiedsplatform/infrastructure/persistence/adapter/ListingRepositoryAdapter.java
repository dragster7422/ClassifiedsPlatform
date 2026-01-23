package com.classifiedsplatform.infrastructure.persistence.adapter;

import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.domain.model.Listing;
import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.ListingStatus;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingEntity;
import com.classifiedsplatform.infrastructure.persistence.mapper.ListingEntityMapper;
import com.classifiedsplatform.infrastructure.persistence.repository.JpaListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class ListingRepositoryAdapter implements ListingRepository {

    private final JpaListingRepository jpaRepository;
    private final ListingEntityMapper mapper;

    public ListingRepositoryAdapter(JpaListingRepository jpaRepository, ListingEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Listing save(Listing listing) {
        Optional<ListingEntity> existingEntity = jpaRepository.findById(listing.getId());
        ListingEntity entity;

        if (existingEntity.isPresent()) {
            // Update existing entity
            entity = existingEntity.get();
            mapper.updateEntity(listing, entity);
        } else {
            // Create new entity
            entity = mapper.toEntity(listing);
        }

        ListingEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Listing> findByFilters(
            String query,
            Category category,
            ListingStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        return jpaRepository.findByFilters(query, category, status, minPrice, maxPrice, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(Listing listing) {
        jpaRepository.deleteById(listing.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}