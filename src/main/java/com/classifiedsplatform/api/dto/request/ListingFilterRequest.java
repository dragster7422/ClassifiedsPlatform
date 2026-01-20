package com.classifiedsplatform.api.dto.request;

import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record ListingFilterRequest(
        String query,
        Category category,
        ListingStatus status,

        @DecimalMin(value = "0.0", message = "Min price cannot be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0.0", message = "Max price cannot be negative")
        BigDecimal maxPrice,

        @Min(value = 0, message = "Page cannot be negative")
        Integer page,

        @Min(value = 1, message = "Size must be at least 1")
        @Max(value = 100, message = "Size cannot exceed 100")
        Integer size,

        String sortBy,
        String sortDirection
) {
    public ListingFilterRequest {
        // Default values
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "createdAt";
        if (sortDirection == null) sortDirection = "desc";
    }
}