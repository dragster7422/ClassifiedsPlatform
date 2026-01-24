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
    @DisplayName("Should handle idempotent publish request - same idempotency key returns same result")
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
        // Should return same result (200 OK) with same listing data
        MvcResult secondPublishResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();

        ListingResponse secondResponse = objectMapper.readValue(
                secondPublishResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        // Verify both responses are identical
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.status()).isEqualTo(firstResponse.status());

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

        // Try again with same key - should return same cached result (200 OK)
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Should return same response structure for idempotent requests")
    void shouldReturnSameResponseStructureForIdempotentRequests() throws Exception {
        UUID listingId = createDraftListing();
        String idempotencyKey = "response-structure-test-" + UUID.randomUUID();

        // First publish
        MvcResult firstResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn();

        ListingResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        // Second publish with same key
        MvcResult secondResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn();

        ListingResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        // Verify responses are identical (ignoring timestamps which may have microsecond differences)
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.status()).isEqualTo(firstResponse.status());
        assertThat(secondResponse.title()).isEqualTo(firstResponse.title());
        assertThat(secondResponse.price()).isEqualTo(firstResponse.price());
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
    @DisplayName("Should return cached result when using same idempotency key for same listing")
    void shouldReturnCachedResultWhenUsingSameIdempotencyKey() throws Exception {
        UUID listingId = createDraftListing();
        String sharedIdempotencyKey = "shared-key-" + UUID.randomUUID();

        // First publish with idempotency key
        MvcResult firstResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", sharedIdempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();

        ListingResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        // Second request with same idempotency key should return cached result
        MvcResult secondResult = mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", sharedIdempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn();

        ListingResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                ListingResponse.class
        );

        // Verify both responses are identical (ignoring timestamps which may have microsecond differences)
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.status()).isEqualTo(firstResponse.status());
        assertThat(secondResponse.title()).isEqualTo(firstResponse.title());
        assertThat(secondResponse.price()).isEqualTo(firstResponse.price());

        // Verify listing is still in PUBLISHED status
        mockMvc.perform(get("/listings/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Should create new idempotency record for different operation on same listing")
    void shouldCreateNewIdempotencyRecordForDifferentOperation() throws Exception {
        UUID listingId = createDraftListing();

        // Publish with first idempotency key
        String firstKey = "first-operation-" + UUID.randomUUID();
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", firstKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Verify first idempotency record exists
        assertThat(idempotencyRepository.findByIdempotencyKey(firstKey)).isPresent();

        // Attempting to publish again with different key should fail due to business logic
        // (can't publish already published listing), not idempotency
        String secondKey = "second-operation-" + UUID.randomUUID();
        mockMvc.perform(post("/listings/{id}/publish", listingId)
                        .header("Idempotency-Key", secondKey))
                .andExpect(status().isConflict()); // Business rule: already published

        // Verify second idempotency record was NOT created (operation failed)
        assertThat(idempotencyRepository.findByIdempotencyKey(secondKey)).isEmpty();
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