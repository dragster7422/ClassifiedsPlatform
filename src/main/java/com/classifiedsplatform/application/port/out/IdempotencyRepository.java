package com.classifiedsplatform.application.port.out;

import com.classifiedsplatform.domain.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRepository {

    IdempotencyRecord save(IdempotencyRecord record);

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    void deleteExpiredRecords();
}