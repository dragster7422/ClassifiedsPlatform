package com.classifiedsplatform.api.mapper;

import com.classifiedsplatform.api.dto.response.PhotoResponse;
import com.classifiedsplatform.application.port.in.UploadListingPhotoCommand;
import com.classifiedsplatform.domain.model.ListingPhoto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
public class PhotoDtoMapper {

    public UploadListingPhotoCommand toCommand(UUID listingId, MultipartFile file) throws IOException {
        return new UploadListingPhotoCommand(
                listingId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes()
        );
    }

    public PhotoResponse toResponse(ListingPhoto photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getMetadata().getFilename(),
                photo.getMetadata().getContentType(),
                photo.getMetadata().getSize(),
                photo.getCreatedAt()
        );
    }
}