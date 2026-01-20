package com.classifiedsplatform.application.port.out;

import com.classifiedsplatform.domain.model.Listing;
import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findById(UUID id);

    Page<Listing> findByFilters(
            String query,
            Category category,
            ListingStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    void delete(Listing listing);

    boolean existsById(UUID id);
}