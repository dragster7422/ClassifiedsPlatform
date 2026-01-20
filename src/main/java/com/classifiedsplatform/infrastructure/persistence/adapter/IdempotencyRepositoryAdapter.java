package com.classifiedsplatform.infrastructure.persistence.adapter;

import com.classifiedsplatform.application.port.out.IdempotencyRepository;
import com.classifiedsplatform.domain.model.IdempotencyRecord;
import com.classifiedsplatform.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.classifiedsplatform.infrastructure.persistence.mapper.IdempotencyRecordEntityMapper;
import com.classifiedsplatform.infrastructure.persistence.repository.JpaIdempotencyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Transactional
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private final JpaIdempotencyRepository jpaRepository;
    private final IdempotencyRecordEntityMapper mapper;

    public IdempotencyRepositoryAdapter(JpaIdempotencyRepository jpaRepository, IdempotencyRecordEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = mapper.toEntity(record);
        IdempotencyRecordEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteExpiredRecords() {
        jpaRepository.deleteExpiredRecords(LocalDateTime.now());
    }
}