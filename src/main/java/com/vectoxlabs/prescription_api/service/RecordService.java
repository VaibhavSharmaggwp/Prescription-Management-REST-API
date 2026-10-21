package com.vectoxlabs.prescription_api.service;

import com.vectoxlabs.prescription_api.dto.CreateRecordRequest;
import com.vectoxlabs.prescription_api.dto.RecordDetailsResponse;
import com.vectoxlabs.prescription_api.dto.RecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecordService {
    RecordResponse createRecord(CreateRecordRequest request);
    RecordDetailsResponse getRecordById(Long id);
    Page<RecordDetailsResponse> getAllRecords(Pageable pageable);
}
