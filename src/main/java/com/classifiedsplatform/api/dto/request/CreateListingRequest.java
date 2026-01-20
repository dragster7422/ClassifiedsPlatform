package com.classifiedsplatform.api.dto.request;

import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateListingRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
        String title,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "Price format is invalid")
        BigDecimal price,

        @NotNull(message = "Currency is required")
        Currency currency,

        @NotNull(message = "Category is required")
        Category category
) {
}