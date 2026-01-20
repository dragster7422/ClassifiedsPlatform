package com.classifiedsplatform.application.usecase;

import com.classifiedsplatform.application.port.in.UploadListingPhotoCommand ;
import com.classifiedsplatform.application.port.out.FileStoragePort;
import com.classifiedsplatform.application.port.out.ListingRepository;
import com.classifiedsplatform.application.port.out.ListingPhotoRepository;
import com.classifiedsplatform.application.service.AuditLogService;
import com.classifiedsplatform.domain.event.PhotoUploadedEvent;
import com.classifiedsplatform.domain.exception.ListingNotFoundException;
import com.classifiedsplatform.domain.exception.ListingPhotoLimitExceededException;
import com.classifiedsplatform.domain.model.Listing;
import com.classifiedsplatform.domain.model.ListingPhoto;
import com.classifiedsplatform.domain.model.vo.PhotoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Transactional
public class UploadListingPhotoUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadListingPhotoUseCase.class);

    private final ListingRepository listingRepository;
    private final ListingPhotoRepository photoRepository;
    private final FileStoragePort fileStorage;
    private final AuditLogService auditLogService;

    public UploadListingPhotoUseCase(
            ListingRepository listingRepository,
            ListingPhotoRepository photoRepository,
            FileStoragePort fileStorage,
            AuditLogService auditLogService
    ) {
        this.listingRepository = listingRepository;
        this.photoRepository = photoRepository;
        this.fileStorage = fileStorage;
        this.auditLogService = auditLogService;
    }

    public ListingPhoto execute(UploadListingPhotoCommand command) {
        log.debug("Uploading photo for listing: {}, filename: {}",
                command.listingId(), command.filename());

        // Find listing
        Listing listing = listingRepository.findById(command.listingId())
                .orElseThrow(() -> new ListingNotFoundException(command.listingId()));

        // Check if can add more photos
        if (!listing.canAddMorePhotos()) {
            throw new ListingPhotoLimitExceededException(listing.getMaxPhotosAllowed());
        }

        // Create photo metadata (validates format and size)
        PhotoMetadata metadata = PhotoMetadata.of(
                command.filename(),
                command.contentType(),
                command.fileSize()
        );

        // Store file
        String storagePath;
        try {
            storagePath = fileStorage.store(command.filename(), command.fileData());
        } catch (IOException e) {
            log.error("Failed to store file: {}", command.filename(), e);
            throw new RuntimeException("Failed to store file", e);
        }

        // Create photo domain object
        ListingPhoto photo = ListingPhoto.create(command.listingId(), metadata, storagePath);

        // Save photo
        ListingPhoto savedPhoto = photoRepository.save(photo);

        // Add photo to listing (updates listing's updatedAt)
        listing.addPhoto(savedPhoto);
        listingRepository.save(listing);

        // Create and save audit log
        PhotoUploadedEvent event = new PhotoUploadedEvent(
                savedPhoto.getId(),
                savedPhoto.getListingId(),
                savedPhoto.getMetadata().getFilename(),
                savedPhoto.getMetadata().getSize(),
                LocalDateTime.now()
        );
        auditLogService.logPhotoUploaded(event);

        log.info("Photo uploaded successfully: {}", savedPhoto.getId());
        return savedPhoto;
    }
}