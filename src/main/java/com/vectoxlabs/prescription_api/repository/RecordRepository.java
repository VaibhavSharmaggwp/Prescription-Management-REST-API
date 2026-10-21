package com.vectoxlabs.prescription_api.repository;

import com.vectoxlabs.prescription_api.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecordRepository extends JpaRepository<Record, Long> {
    //  We will write idempotency checks here later
    Optional<Record> findByIdempotencyKey(String idempotencyKey);
}
