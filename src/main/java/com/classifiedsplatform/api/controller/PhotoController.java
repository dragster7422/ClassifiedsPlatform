package com.classifiedsplatform.api.controller;

import com.classifiedsplatform.api.dto.response.PhotoResponse;
import com.classifiedsplatform.api.mapper.PhotoDtoMapper;
import com.classifiedsplatform.application.port.in.UploadListingPhotoCommand;
import com.classifiedsplatform.application.usecase.UploadListingPhotoUseCase;
import com.classifiedsplatform.domain.model.ListingPhoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/listings/{listingId}/photos")
public class PhotoController {

    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);

    private final UploadListingPhotoUseCase uploadListingPhotoUseCase;
    private final PhotoDtoMapper mapper;

    public PhotoController(UploadListingPhotoUseCase uploadListingPhotoUseCase, PhotoDtoMapper mapper) {
        this.uploadListingPhotoUseCase = uploadListingPhotoUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @PathVariable UUID listingId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.debug("Uploading photo for listing: {}, filename: {}", listingId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        UploadListingPhotoCommand command = mapper.toCommand(listingId, file);
        ListingPhoto photo = uploadListingPhotoUseCase.execute(command);
        PhotoResponse response = mapper.toResponse(photo);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}