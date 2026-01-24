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
import java.util.List;
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
    public ResponseEntity<List<PhotoResponse>> uploadPhotos(
            @PathVariable UUID listingId,
            @RequestParam("files") MultipartFile[] files
    ) throws IOException {
        log.debug("Uploading {} photos for listing: {}", files.length, listingId);

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Files list cannot be null or empty");
        }

        List<UploadListingPhotoCommand> commands = mapper.toCommands(listingId, files);
        List<ListingPhoto> photos = uploadListingPhotoUseCase.execute(commands);
        List<PhotoResponse> responses = photos.stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}