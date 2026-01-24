package com.classifiedsplatform.integration;

import com.classifiedsplatform.api.dto.request.CreateListingRequest;
import com.classifiedsplatform.api.dto.response.ListingDetailResponse;
import com.classifiedsplatform.api.dto.response.ListingResponse;
import com.classifiedsplatform.api.dto.response.PhotoResponse;
import com.classifiedsplatform.application.port.out.AuditLogRepository;
import com.classifiedsplatform.application.port.out.FileStoragePort;
import com.classifiedsplatform.domain.model.AuditLog;
import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.Currency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integration Test: Complete Listing Lifecycle")
class ListingLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FileStoragePort fileStoragePort;

    private Path testUploadDir;

    @BeforeEach
    void setUp() throws Exception {
        testUploadDir = Paths.get("./test-uploads/listing-images");
        Files.createDirectories(testUploadDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up uploaded files
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("Should complete full listing lifecycle: create -> upload photos (batch) -> publish")
    void shouldCompleteFullListingLifecycle() throws Exception {
        // ========== STEP 1: Create Listing ==========
        CreateListingRequest createRequest = new CreateListingRequest(
                "iPhone 15 Pro Max",
                "Brand new iPhone 15 Pro Max, 256GB, Blue Titanium. Never used, still in box.",
                new BigDecimal("1299.99"),
                Currency.USD,
                Category.ELECTRONICS
        );

        MvcResult createResult = mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("iPhone 15 Pro Max"))
                .andExpect(jsonPath("$.price").value(1299.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.category").value("ELECTRONICS"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.photoCount").value(0))
                .andReturn();

        String createResponseJson = createResult.getResponse().getContentAsString();
        ListingResponse listingResponse = objectMapper.readValue(createResponseJson, ListingResponse.class);
        UUID listingId = listingResponse.id();

        assertThat(listingId).isNotNull();

        // ========== STEP 2: Upload 3 Photos in One Batch ==========
        byte[] photoData1 = createTestImageData("photo1.jpg");
        byte[] photoData2 = createTestImageData("photo2.jpg");
        byte[] photoData3 = createTestImageData("photo3.jpg");

        MockMultipartFile photo1 = new MockMultipartFile(
                "files",
                "iphone-front.jpg",
                "image/jpeg",
                photoData1
        );

        MockMultipartFile photo2 = new MockMultipartFile(
                "files",
                "iphone-back.jpg",
                "image/jpeg",
                photoData2
        );

        MockMultipartFile photo3 = new MockMultipartFile(
                "files",
                "iphone-box.jpg",
                "image/png",
                photoData3
        );

        MvcResult photosResult = mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo1)
                        .file(photo2)
                        .file(photo3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[1].id").exists())
                .andExpect(jsonPath("$[2].id").exists())
                .andReturn();

        List<PhotoResponse> photoResponses = objectMapper.readValue(
                photosResult.getResponse().getContentAsString(),
                new TypeReference<List<PhotoResponse>>() {}
        );

        assertThat(photoResponses).hasSize(3);
        assertThat(photoResponses)
                .extracting(PhotoResponse::filename)
                .containsExactlyInAnyOrder("iphone-front.jpg", "iphone-back.jpg", "iphone-box.jpg");

        // ========== STEP 3: Verify Listing Has 3 Photos ==========
        MvcResult detailsResult = mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos.length()").value(3))
                .andReturn();

        ListingDetailResponse detailsResponse = objectMapper.readValue(
                detailsResult.getResponse().getContentAsString(),
                ListingDetailResponse.class
        );

        assertThat(detailsResponse.photos()).hasSize(3);
        assertThat(detailsResponse.photos())
                .extracting(PhotoResponse::filename)
                .containsExactlyInAnyOrder("iphone-front.jpg", "iphone-back.jpg", "iphone-box.jpg");

        // ========== STEP 4: Publish Listing ==========
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // ========== STEP 5: Verify Listing is Published ==========
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // ========== STEP 6: Verify Audit Logs Created ==========
        List<AuditLog> auditLogs = auditLogRepository.findByListingId(listingId);

        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs).hasSizeGreaterThanOrEqualTo(4); // 3 photo uploads + 1 publish

        // Verify photo upload events
        long photoUploadEvents = auditLogs.stream()
                .filter(log -> "PHOTO_UPLOADED".equals(log.getEventType()))
                .count();
        assertThat(photoUploadEvents).isEqualTo(3);

        // Verify listing published event
        long publishedEvents = auditLogs.stream()
                .filter(log -> "LISTING_PUBLISHED".equals(log.getEventType()))
                .count();
        assertThat(publishedEvents).isEqualTo(1);

        // ========== STEP 7: Verify Listing Appears in Published Search Results ==========
        mockMvc.perform(get("/listings")
                        .param("status", "PUBLISHED")
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.id == '" + listingId + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + listingId + "')].photoCount").value(3));
    }

    @Test
    @DisplayName("Should enforce photo upload limits and validation rules")
    void shouldEnforcePhotoUploadLimitsAndValidation() throws Exception {
        // Create listing
        CreateListingRequest createRequest = new CreateListingRequest(
                "Test Product",
                "Test description",
                new BigDecimal("100.00"),
                Currency.UAH,
                Category.OTHER
        );

        MvcResult createResult = mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ListingResponse listingResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                ListingResponse.class
        );
        UUID listingId = listingResponse.id();

        // ========== Test 1: Upload 10 photos in one batch (max limit) ==========
        MockMultipartFile[] photos = new MockMultipartFile[10];
        for (int i = 0; i < 10; i++) {
            byte[] photoData = createTestImageData("photo" + i + ".jpg");
            photos[i] = new MockMultipartFile(
                    "files",
                    "photo-" + i + ".jpg",
                    "image/jpeg",
                    photoData
            );
        }

        var requestBuilder = multipart("/listings/{listingId}/photos", listingId);
        for (MockMultipartFile photo : photos) {
            requestBuilder.file(photo);
        }

        mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(10));

        // Verify 10 photos uploaded
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(10));

        // ========== Test 2: Try to upload 11th photo (should fail) ==========
        byte[] photoData11 = createTestImageData("photo11.jpg");
        MockMultipartFile photo11 = new MockMultipartFile(
                "files",
                "photo-11.jpg",
                "image/jpeg",
                photoData11
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Cannot add 1 photo(s). Listing already has 10 photo(s) and maximum allowed is 10")));

        // ========== Test 3: Try to upload batch that exceeds limit ==========
        UUID newListingId = createNewListing();

        // Upload 8 photos first
        var builder1 = multipart("/listings/{listingId}/photos", newListingId);
        for (int i = 0; i < 8; i++) {
            byte[] photoData = createTestImageData("batch1-photo" + i + ".jpg");
            builder1.file(new MockMultipartFile("files", "batch1-photo-" + i + ".jpg", "image/jpeg", photoData));
        }
        mockMvc.perform(builder1).andExpect(status().isCreated());

        // Try to upload 3 more (total would be 11)
        var builder2 = multipart("/listings/{listingId}/photos", newListingId);
        for (int i = 0; i < 3; i++) {
            byte[] photoData = createTestImageData("batch2-photo" + i + ".jpg");
            builder2.file(new MockMultipartFile("files", "batch2-photo-" + i + ".jpg", "image/jpeg", photoData));
        }

        mockMvc.perform(builder2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Cannot add 3 photo(s). Listing already has 8 photo(s) and maximum allowed is 10")));

        // ========== Test 4: Try to upload invalid file type ==========
        byte[] invalidFileData = "not an image".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "files",
                "document.pdf",
                "application/pdf",
                invalidFileData
        );

        UUID anotherListingId = createNewListing();

        mockMvc.perform(multipart("/listings/{listingId}/photos", anotherListingId)
                        .file(invalidFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Invalid content type")));

        // ========== Test 5: Try to upload file that's too large (>2MB) ==========
        byte[] largeFileData = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "files",
                "large-photo.jpg",
                "image/jpeg",
                largeFileData
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", anotherListingId)
                        .file(largeFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("File size exceeds maximum allowed size")));

        // ========== Test 6: Try to upload more than 10 files in one request ==========
        UUID yetAnotherListingId = createNewListing();

        var builder3 = multipart("/listings/{listingId}/photos", yetAnotherListingId);
        for (int i = 0; i < 11; i++) {
            byte[] photoData = createTestImageData("many-photo" + i + ".jpg");
            builder3.file(new MockMultipartFile("files", "many-photo-" + i + ".jpg", "image/jpeg", photoData));
        }

        mockMvc.perform(builder3)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Cannot add 11 photo(s). Listing already has 0 photo(s) and maximum allowed is 10")));
    }

    @Test
    @DisplayName("Should not allow publishing already published listing")
    void shouldNotAllowPublishingAlreadyPublishedListing() throws Exception {
        // Create and publish listing
        UUID listingId = createNewListing();

        String idempotencyKey = UUID.randomUUID().toString();
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Try to publish again with different idempotency key
        String newIdempotencyKey = UUID.randomUUID().toString();
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", newIdempotencyKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Cannot transition from PUBLISHED to PUBLISHED")));
    }

    @Test
    @DisplayName("Should handle batch upload with mixed valid and empty files")
    void shouldHandleBatchUploadWithMixedFiles() throws Exception {
        UUID listingId = createNewListing();

        byte[] photoData1 = createTestImageData("photo1.jpg");
        byte[] emptyData = new byte[0];
        byte[] photoData2 = createTestImageData("photo2.jpg");

        MockMultipartFile photo1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", photoData1);
        MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.jpg", "image/jpeg", emptyData);
        MockMultipartFile photo2 = new MockMultipartFile("files", "photo2.jpg", "image/jpeg", photoData2);

        // Should skip empty file and upload only valid ones
        mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo1)
                        .file(emptyFile)
                        .file(photo2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        // Verify only 2 photos uploaded
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(2));
    }

    // ========== Helper Methods ==========

    private UUID createNewListing() throws Exception {
        CreateListingRequest request = new CreateListingRequest(
                "Test Listing",
                "Test Description",
                new BigDecimal("50.00"),
                Currency.UAH,
                Category.OTHER
        );

        MvcResult result = mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ListingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ListingResponse.class
        );

        return response.id();
    }

    private byte[] createTestImageData(String identifier) {
        // Create simple test image data (minimal valid JPEG header + some data)
        byte[] jpegHeader = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG SOI and APP0
                0x00, 0x10, // APP0 length
                0x4A, 0x46, 0x49, 0x46, 0x00, // "JFIF\0"
                0x01, 0x01, // Version
                0x00, // Units
                0x00, 0x01, 0x00, 0x01, // X and Y density
                0x00, 0x00 // Thumbnail size
        };

        byte[] data = new byte[1024]; // Small 1KB image
        System.arraycopy(jpegHeader, 0, data, 0, jpegHeader.length);

        // Fill with identifier to make unique
        byte[] identifierBytes = identifier.getBytes();
        System.arraycopy(identifierBytes, 0, data, jpegHeader.length,
                Math.min(identifierBytes.length, data.length - jpegHeader.length - 2));

        // Add JPEG EOI marker at the end
        data[data.length - 2] = (byte) 0xFF;
        data[data.length - 1] = (byte) 0xD9;

        return data;
    }
}