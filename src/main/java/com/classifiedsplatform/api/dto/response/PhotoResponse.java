package com.classifiedsplatform.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        String filename,
        String contentType,
        long size,
        LocalDateTime createdAt
) {
}