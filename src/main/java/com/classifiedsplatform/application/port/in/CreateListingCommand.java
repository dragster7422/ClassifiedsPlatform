package com.classifiedsplatform.application.port.in;

import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.Currency;

import java.math.BigDecimal;

public record CreateListingCommand(
        String title,
        String description,
        BigDecimal priceAmount,
        Currency priceCurrency,
        Category category
) {
    public CreateListingCommand {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (priceAmount == null) {
            throw new IllegalArgumentException("Price amount cannot be null");
        }
        if (priceCurrency == null) {
            throw new IllegalArgumentException("Price currency cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
    }
}