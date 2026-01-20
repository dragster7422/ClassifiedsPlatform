package com.classifiedsplatform.api.controller;

import com.classifiedsplatform.api.dto.request.CreateListingRequest;
import com.classifiedsplatform.api.dto.request.ListingFilterRequest;
import com.classifiedsplatform.api.dto.response.ListingDetailResponse;
import com.classifiedsplatform.api.dto.response.ListingResponse;
import com.classifiedsplatform.api.dto.response.PageResponse;
import com.classifiedsplatform.api.mapper.ListingDtoMapper;
import com.classifiedsplatform.application.port.in.CreateListingCommand;
import com.classifiedsplatform.application.port.in.GetListingsQuery;
import com.classifiedsplatform.application.port.in.PublishListingCommand;
import com.classifiedsplatform.application.usecase.*;
import com.classifiedsplatform.domain.model.Listing;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private static final Logger log = LoggerFactory.getLogger(ListingController.class);

    private final CreateListingUseCase createListingUseCase;
    private final PublishListingUseCase publishListingUseCase;
    private final GetListingsUseCase getListingsUseCase;
    private final GetListingDetailsUseCase getListingDetailsUseCase;
    private final ListingDtoMapper mapper;

    public ListingController(
            CreateListingUseCase createListingUseCase,
            PublishListingUseCase publishListingUseCase,
            GetListingsUseCase getListingsUseCase,
            GetListingDetailsUseCase getListingDetailsUseCase,
            ListingDtoMapper mapper
    ) {
        this.createListingUseCase = createListingUseCase;
        this.publishListingUseCase = publishListingUseCase;
        this.getListingsUseCase = getListingsUseCase;
        this.getListingDetailsUseCase = getListingDetailsUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        log.debug("Creating listing: {}", request.title());

        CreateListingCommand command = mapper.toCommand(request);
        Listing listing = createListingUseCase.execute(command);
        ListingResponse response = mapper.toResponse(listing);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ListingResponse> publishListing(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.debug("Publishing listing: {}, idempotencyKey: {}", id, idempotencyKey);

        PublishListingCommand command = new PublishListingCommand(id, idempotencyKey);
        Listing listing = publishListingUseCase.execute(command);
        ListingResponse response = mapper.toResponse(listing);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ListingResponse>> getListings(
            @Valid @ModelAttribute ListingFilterRequest request
    ) {
        log.debug("Getting listings with filters: {}", request);

        GetListingsQuery query = new GetListingsQuery(
                request.query(),
                request.category(),
                request.status(),
                request.minPrice(),
                request.maxPrice(),
                request.page(),
                request.size(),
                request.sortBy(),
                request.sortDirection()
        );

        Page<Listing> page = getListingsUseCase.execute(query);
        PageResponse<ListingResponse> response = mapper.toPageResponse(page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> getListingDetails(@PathVariable UUID id) {
        log.debug("Getting listing details: {}", id);

        Listing listing = getListingDetailsUseCase.execute(id);
        ListingDetailResponse response = mapper.toDetailResponse(listing);

        return ResponseEntity.ok(response);
    }
}