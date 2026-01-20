package com.classifiedsplatform.application.service;

import com.classifiedsplatform.application.port.out.IdempotencyRepository;
import com.classifiedsplatform.domain.model.IdempotencyRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        Optional<IdempotencyRecord> record = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (record.isPresent() && record.get().isExpired()) {
            log.debug("Idempotency record expired for key: {}", idempotencyKey);
            return Optional.empty();
        }

        return record;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRecord(String idempotencyKey, UUID listingId, int httpStatus) {
        try {
            // Create simple result JSON
            Map<String, Object> result = Map.of(
                    "listingId", listingId.toString(),
                    "status", "success"
            );
            String resultJson = objectMapper.writeValueAsString(result);

            IdempotencyRecord record = IdempotencyRecord.create(
                    idempotencyKey,
                    listingId,
                    resultJson,
                    httpStatus
            );

            idempotencyRepository.save(record);
            log.debug("Idempotency record saved for key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result to JSON", e);
            throw new RuntimeException("Failed to save idempotency record", e);
        }
    }

    @Transactional
    public void cleanupExpiredRecords() {
        log.info("Cleaning up expired idempotency records");
        idempotencyRepository.deleteExpiredRecords();
    }
}