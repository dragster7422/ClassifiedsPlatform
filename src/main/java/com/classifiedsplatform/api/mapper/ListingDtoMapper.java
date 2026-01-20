package com.classifiedsplatform.api.mapper;

import com.classifiedsplatform.api.dto.request.CreateListingRequest;
import com.classifiedsplatform.api.dto.response.ListingDetailResponse;
import com.classifiedsplatform.api.dto.response.ListingResponse;
import com.classifiedsplatform.api.dto.response.PageResponse;
import com.classifiedsplatform.api.dto.response.PhotoResponse;
import com.classifiedsplatform.application.port.in.CreateListingCommand;
import com.classifiedsplatform.domain.model.Listing;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListingDtoMapper {

    private final PhotoDtoMapper photoMapper;

    public ListingDtoMapper(PhotoDtoMapper photoMapper) {
        this.photoMapper = photoMapper;
    }

    public CreateListingCommand toCommand(CreateListingRequest request) {
        return new CreateListingCommand(
                request.title(),
                request.description(),
                request.price(),
                request.currency(),
                request.category()
        );
    }

    public ListingResponse toResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice().getAmount(),
                listing.getPrice().getCurrency(),
                listing.getCategory(),
                listing.getStatus(),
                listing.getPhotoCount(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    public ListingDetailResponse toDetailResponse(Listing listing) {
        List<PhotoResponse> photos = listing.getPhotos().stream()
                .map(photoMapper::toResponse)
                .collect(Collectors.toList());

        return new ListingDetailResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice().getAmount(),
                listing.getPrice().getCurrency(),
                listing.getCategory(),
                listing.getStatus(),
                photos,
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                listing.getVersion()
        );
    }

    public PageResponse<ListingResponse> toPageResponse(Page<Listing> page) {
        List<ListingResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}