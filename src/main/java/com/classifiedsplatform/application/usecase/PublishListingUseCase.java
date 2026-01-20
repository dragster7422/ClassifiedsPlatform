package com.classifiedsplatform.application.usecase;

import com.classifiedsplatform.application.port.in.PublishListingCommand;
import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.application.service.AuditLogService;
import com.classifiedsplatform.application.service.IdempotencyService;
import com.classifiedsplatform.domain.event.ListingPublishedEvent;
import com.classifiedsplatform.domain.exception.IdempotencyConflictException;
import com.classifiedsplatform.domain.exception.ListingNotFoundException;
import com.classifiedsplatform.domain.model.IdempotencyRecord;
import com.classifiedsplatform.domain.model.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class PublishListingUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublishListingUseCase.class);

    private final ListingRepository listingRepository;
    private final IdempotencyService idempotencyService;
    private final AuditLogService auditLogService;

    public PublishListingUseCase(
            ListingRepository listingRepository,
            IdempotencyService idempotencyService,
            AuditLogService auditLogService
    ) {
        this.listingRepository = listingRepository;
        this.idempotencyService = idempotencyService;
        this.auditLogService = auditLogService;
    }

    public Listing execute(PublishListingCommand command) {
        log.debug("Executing publish listing for id: {}, idempotencyKey: {}",
                command.listingId(), command.idempotencyKey());

        // Check idempotency
        if (command.idempotencyKey() != null) {
            Optional<IdempotencyRecord> existingRecord =
                    idempotencyService.findByKey(command.idempotencyKey());

            if (existingRecord.isPresent()) {
                log.warn("Idempotent request conflict detected for key: {}",
                        command.idempotencyKey());
                throw new IdempotencyConflictException(
                        command.idempotencyKey(),
                        existingRecord.get().getListingId());
            }
        }

        // Find listing
        Listing listing = listingRepository.findById(command.listingId())
                .orElseThrow(() -> new ListingNotFoundException(command.listingId()));

        // Publish (domain logic with state validation)
        listing.publish();

        // Save
        Listing publishedListing = listingRepository.save(listing);

        // Create and save audit log
        ListingPublishedEvent event = new ListingPublishedEvent(
                publishedListing.getId(),
                publishedListing.getTitle(),
                LocalDateTime.now()
        );
        auditLogService.logListingPublished(event);

        // Save idempotency record
        if (command.idempotencyKey() != null) {
            idempotencyService.saveRecord(
                    command.idempotencyKey(),
                    publishedListing.getId(),
                    200
            );
        }

        log.info("Listing published successfully: {}", publishedListing.getId());
        return publishedListing;
    }
}