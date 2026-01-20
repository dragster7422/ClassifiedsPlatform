package com.classifiedsplatform.api.dto.response;

import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.Currency;
import com.classifiedsplatform.domain.model.vo.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        Currency currency,
        Category category,
        ListingStatus status,
        int photoCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}