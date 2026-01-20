package com.classifiedsplatform.domain.model;

import com.classifiedsplatform.domain.exception.InvalidStateTransitionException;
import com.classifiedsplatform.domain.exception.ListingPhotoLimitExceededException;
import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.ListingStatus;
import com.classifiedsplatform.domain.model.vo.Money;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Listing {

    private static final int MAX_PHOTOS = 10;
    private static final int MIN_TITLE_LENGTH = 3;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private UUID id;
    private String title;
    private String description;
    private Money price;
    private Category category;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private final List<ListingPhoto> photos;

    private Listing() {
        this.photos = new ArrayList<>();
    }

    // Factory method for creating NEW listings (business logic)
    public static Listing create(String title, String description, Money price, Category category) {
        validatePrice(price);
        validateCategory(category);

        Listing listing = new Listing();
        listing.id = UUID.randomUUID();
        listing.title = validateAndNormalizeTitle(title);
        listing.description = validateAndNormalizeDescription(description);
        listing.price = price;
        listing.category = category;
        listing.status = ListingStatus.DRAFT;
        listing.createdAt = LocalDateTime.now();
        listing.updatedAt = LocalDateTime.now();
        listing.version = null;
        return listing;
    }

    // Factory method for RECONSTITUTING from persistence layer
    public static Listing reconstitute(
            UUID id,
            String title,
            String description,
            Money price,
            Category category,
            ListingStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version,
            List<ListingPhoto> photos
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null when reconstituting");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null when reconstituting");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null when reconstituting");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("UpdatedAt cannot be null when reconstituting");
        }
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null when reconstituting");
        }

        Listing listing = new Listing();
        listing.id = id;
        listing.title = title;
        listing.description = description;
        listing.price = price;
        listing.category = category;
        listing.status = status;
        listing.createdAt = createdAt;
        listing.updatedAt = updatedAt;
        listing.version = version;

        if (photos != null) {
            listing.photos.addAll(photos);
        }

        return listing;
    }

    // Business methods
    public void publish() {
        if (this.status != ListingStatus.DRAFT) {
            throw new InvalidStateTransitionException(this.status, ListingStatus.PUBLISHED);
        }
        this.status = ListingStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        if (this.status != ListingStatus.PUBLISHED) {
            throw new InvalidStateTransitionException(this.status, ListingStatus.ARCHIVED);
        }
        this.status = ListingStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTitle(String newTitle) {
        this.title = validateAndNormalizeTitle(newTitle);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String newDescription) {
        this.description = validateAndNormalizeDescription(newDescription);
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePrice(Money newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCategory(Category newCategory) {
        validateCategory(newCategory);
        this.category = newCategory;
        this.updatedAt = LocalDateTime.now();
    }

    public void addPhoto(ListingPhoto photo) {
        if (photo == null) {
            throw new IllegalArgumentException("Photo cannot be null");
        }
        if (photos.size() >= MAX_PHOTOS) {
            throw new ListingPhotoLimitExceededException(MAX_PHOTOS);
        }
        this.photos.add(photo);
        this.updatedAt = LocalDateTime.now();
    }

    // Validation methods
    private static String validateAndNormalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() < MIN_TITLE_LENGTH || trimmedTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Title must be between %d and %d characters", MIN_TITLE_LENGTH, MAX_TITLE_LENGTH)
            );
        }
        return trimmedTitle;
    }

    private static String validateAndNormalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Description cannot exceed %d characters", MAX_DESCRIPTION_LENGTH)
            );
        }
        return trimmed;
    }

    private static void validatePrice(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
    }

    private static void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
    }

    // Query methods
    public boolean isDraft() {
        return this.status == ListingStatus.DRAFT;
    }

    public boolean isPublished() {
        return this.status == ListingStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return this.status == ListingStatus.ARCHIVED;
    }

    public boolean canAddMorePhotos() {
        return photos.size() < MAX_PHOTOS;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<ListingPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    public int getPhotoCount() {
        return photos.size();
    }

    public int getMaxPhotosAllowed() {
        return MAX_PHOTOS;
    }
}