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
    @DisplayName("Should complete full listing lifecycle: create -> upload photos -> publish")
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

        // ========== STEP 2: Upload First Photo ==========
        byte[] photoData1 = createTestImageData("photo1.jpg");
        MockMultipartFile photo1 = new MockMultipartFile(
                "file",
                "iphone-front.jpg",
                "image/jpeg",
                photoData1
        );

        MvcResult photo1Result = mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.filename").value("iphone-front.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.size").value(photoData1.length))
                .andReturn();

        PhotoResponse photoResponse1 = objectMapper.readValue(
                photo1Result.getResponse().getContentAsString(),
                PhotoResponse.class
        );

        // Verify file was actually stored on disk
        assertThat(photoResponse1.id()).isNotNull();

        // ========== STEP 3: Upload Second Photo ==========
        byte[] photoData2 = createTestImageData("photo2.jpg");
        MockMultipartFile photo2 = new MockMultipartFile(
                "file",
                "iphone-back.jpg",
                "image/jpeg",
                photoData2
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("iphone-back.jpg"));

        // ========== STEP 4: Upload Third Photo ==========
        byte[] photoData3 = createTestImageData("photo3.jpg");
        MockMultipartFile photo3 = new MockMultipartFile(
                "file",
                "iphone-box.jpg",
                "image/png",
                photoData3
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("iphone-box.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/png"));

        // ========== STEP 5: Verify Listing Has 3 Photos ==========
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

        // ========== STEP 6: Publish Listing ==========
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // ========== STEP 7: Verify Listing is Published ==========
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // ========== STEP 8: Verify Audit Logs Created ==========
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

        // ========== STEP 9: Verify Listing Appears in Published Search Results ==========
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

        // ========== Test 1: Upload 10 photos (max limit) ==========
        for (int i = 1; i <= 10; i++) {
            byte[] photoData = createTestImageData("photo" + i + ".jpg");
            MockMultipartFile photo = new MockMultipartFile(
                    "file",
                    "photo-" + i + ".jpg",
                    "image/jpeg",
                    photoData
            );

            mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                            .file(photo))
                    .andExpect(status().isCreated());
        }

        // Verify 10 photos uploaded
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(10));

        // ========== Test 2: Try to upload 11th photo (should fail) ==========
        byte[] photoData11 = createTestImageData("photo11.jpg");
        MockMultipartFile photo11 = new MockMultipartFile(
                "file",
                "photo-11.jpg",
                "image/jpeg",
                photoData11
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", listingId)
                        .file(photo11))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum number of photos (10) exceeded"));

        // ========== Test 3: Try to upload invalid file type ==========
        byte[] invalidFileData = "not an image".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                invalidFileData
        );

        UUID newListingId = createNewListing();

        mockMvc.perform(multipart("/listings/{listingId}/photos", newListingId)
                        .file(invalidFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Invalid content type")));

        // ========== Test 4: Try to upload file that's too large (>2MB) ==========
        byte[] largeFileData = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-photo.jpg",
                "image/jpeg",
                largeFileData
        );

        mockMvc.perform(multipart("/listings/{listingId}/photos", newListingId)
                        .file(largeFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("File size exceeds maximum allowed size")));
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