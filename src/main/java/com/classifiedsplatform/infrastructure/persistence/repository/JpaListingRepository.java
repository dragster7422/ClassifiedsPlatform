package com.classifiedsplatform.infrastructure.persistence.repository;

import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.ListingStatus;
import com.classifiedsplatform.infrastructure.persistence.entity.ListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface JpaListingRepository extends JpaRepository<ListingEntity, UUID> {

    @Query("""
        SELECT l FROM ListingEntity l
        WHERE (:query IS NULL OR
               LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:category IS NULL OR l.category = :category)
        AND (:status IS NULL OR l.status = :status)
        AND (:minPrice IS NULL OR l.priceAmount >= :minPrice)
        AND (:maxPrice IS NULL OR l.priceAmount <= :maxPrice)
    """)
    Page<ListingEntity> findByFilters(
            @Param("query") String query,
            @Param("category") Category category,
            @Param("status") ListingStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}