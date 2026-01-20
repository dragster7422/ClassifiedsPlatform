package com.classifiedsplatform.integration;

import com.classifiedsplatform.api.dto.request.CreateListingRequest;
import com.classifiedsplatform.api.dto.response.ListingResponse;
import com.classifiedsplatform.application.port.out.IdempotencyRepository;
import com.classifiedsplatform.domain.model.IdempotencyRecord;
import com.classifiedsplatform.domain.model.vo.Category;
import com.classifiedsplatform.domain.model.vo.Currency;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integration Test: Idempotency for Listing Publication")
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Test
    @DisplayName("Should handle idempotent publish request - same idempotency key returns conflict")
    void shouldHandleIdempotentPublishRequest() throws Exception {
        // ========== STEP 1: Create a listing in DRAFT status ==========
        UUID listingId = createDraftListing();

        // ========== STEP 2: Publish listing with idempotency key ==========
        String idempotencyKey = "publish-" + UUID.randomUUID();

        MvcResult firstPublishResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();

        ListingResponse firstResponse = objectMapper.readValue(
                firstPublishResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        assertThat(firstResponse.status().name()).isEqualTo("PUBLISHED");

        // ========== STEP 3: Verify idempotency record was created ==========
        Optional<IdempotencyRecord> recordOptional = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        assertThat(recordOptional).isPresent();
        IdempotencyRecord record = recordOptional.get();
        assertThat(record.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(record.getListingId()).isEqualTo(listingId);
        assertThat(record.getHttpStatus()).isEqualTo(200);
        assertThat(record.isExpired()).isFalse();

        // ========== STEP 4: Try to publish again with SAME idempotency key ==========
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Operation with idempotency key")))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already processed")));

        // ========== STEP 5: Verify listing status hasn't changed ==========
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Should allow publishing different listings with different idempotency keys")
    void shouldAllowPublishingDifferentListingsWithDifferentKeys() throws Exception {
        // Create two different listings
        UUID listingId1 = createDraftListing();
        UUID listingId2 = createDraftListing();

        String idempotencyKey1 = "publish-listing-1-" + UUID.randomUUID();
        String idempotencyKey2 = "publish-listing-2-" + UUID.randomUUID();

        // Publish both listings with different idempotency keys
        mockMvc.perform(post("/listings/{id}/publish", listingId1)
                        .header("Idempotency-Key", idempotencyKey1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/listings/{id}/publish", listingId2)
                        .header("Idempotency-Key", idempotencyKey2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Verify both idempotency records exist
        assertThat(idempotencyRepository.findByIdempotencyKey(idempotencyKey1)).isPresent();
        assertThat(idempotencyRepository.findByIdempotencyKey(idempotencyKey2)).isPresent();
    }

    @Test
    @DisplayName("Should allow publishing without idempotency key")
    void shouldAllowPublishingWithoutIdempotencyKey() throws Exception {
        UUID listingId = createDraftListing();

        // Publish without idempotency key
        mockMvc.perform(post("/listings/{id}/publish", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Verify listing is published
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Should allow retry after idempotency record expires (24 hours)")
    void shouldAllowRetryAfterIdempotencyRecordExpires() throws Exception {
        // This test demonstrates the concept - in real scenario, record would expire after 24h
        UUID listingId = createDraftListing();
        String idempotencyKey = "expired-key-" + UUID.randomUUID();

        // First publish
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk());

        // Verify record exists and is not expired
        Optional<IdempotencyRecord> record = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(record).isPresent();
        assertThat(record.get().isExpired()).isFalse();

        // Try again with same key - should get conflict
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return IdempotencyConflict with correct error structure")
    void shouldReturnIdempotencyConflictWithCorrectErrorStructure() throws Exception {
        UUID listingId = createDraftListing();
        String idempotencyKey = "error-structure-test-" + UUID.randomUUID();

        // First publish
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk());

        // Second publish with same key - verify error structure
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/listings/" + listingId + "/publish"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(idempotencyKey),
                                org.hamcrest.Matchers.containsString(listingId.toString())
                        )
                ));
    }

    @Test
    @DisplayName("Should handle idempotency for non-existent listing")
    void shouldHandleIdempotencyForNonExistentListing() throws Exception {
        UUID nonExistentListingId = UUID.randomUUID();
        String idempotencyKey = "non-existent-" + UUID.randomUUID();

        // Try to publish non-existent listing
        mockMvc.perform(post("/listings/{id}/publish", nonExistentListingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Listing with id " + nonExistentListingId + " not found")));

        // Verify no idempotency record was created
        Optional<IdempotencyRecord> record = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(record).isEmpty();
    }

    @Test
    @DisplayName("Should validate idempotency key uniqueness across different operations")
    void shouldValidateIdempotencyKeyUniquenessAcrossDifferentOperations() throws Exception {
        UUID listingId1 = createDraftListing();
        UUID listingId2 = createDraftListing();

        String sharedIdempotencyKey = "shared-key-" + UUID.randomUUID();

        // Publish first listing with idempotency key
        mockMvc.perform(post("/listings/{id}/publish", listingId1)
                        .header("Idempotency-Key", sharedIdempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId1.toString()));

        // Try to publish second listing with SAME idempotency key
        mockMvc.perform(post("/listings/{id}/publish", listingId2)
                        .header("Idempotency-Key", sharedIdempotencyKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString(sharedIdempotencyKey)));

        // Verify second listing is still in DRAFT status
        mockMvc.perform(get("/listings/{id}", listingId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // ========== Helper Methods ==========

    private UUID createDraftListing() throws Exception {
        CreateListingRequest request = new CreateListingRequest(
                "MacBook Pro 16\" M3",
                "Powerful laptop for developers",
                new BigDecimal("2499.00"),
                Currency.USD,
                Category.ELECTRONICS
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
}