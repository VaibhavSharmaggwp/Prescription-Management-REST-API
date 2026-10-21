package com.vectoxlabs.prescription_api.service;


import com.vectoxlabs.prescription_api.dto.CreateRecordRequest;
import com.vectoxlabs.prescription_api.dto.RecordDetailsResponse;
import com.vectoxlabs.prescription_api.dto.RecordEntryResponse;
import com.vectoxlabs.prescription_api.dto.RecordResponse;
import com.vectoxlabs.prescription_api.entity.Client;
import com.vectoxlabs.prescription_api.entity.Record;
import com.vectoxlabs.prescription_api.entity.RecordEntry;
import com.vectoxlabs.prescription_api.entity.RecordStatus;
import com.vectoxlabs.prescription_api.entity.User;
import com.vectoxlabs.prescription_api.exception.DuplicateRequestException;
import com.vectoxlabs.prescription_api.exception.ResourceNotFoundException;
import com.vectoxlabs.prescription_api.repository.ClientRepository;
import com.vectoxlabs.prescription_api.repository.RecordRepository;
import com.vectoxlabs.prescription_api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService{
    // Spring automatically injects these because of Lombok's @RequiredArgsConstructor
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional // CRITICAL: Guarantees Record and Entries save together atomically
    public RecordResponse createRecord(CreateRecordRequest request){
        // 1. Idempotency Check: Did we already process this exact request?

        if(recordRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()){
            throw new DuplicateRequestException("A record with this idempotency key already exists.");
        }

        // 2. Fetch User (or throw 404)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // 3. Fetch Client (or throw 404)
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + request.getClientId()));

        // 4. Build the Record Entity
        Record record = new Record();
        record.setUser(user);
        record.setClient(client);
        record.setRecordDate(request.getRecordDate());
        record.setDiagnosis(request.getDiagnosis());
        record.setStatus(RecordStatus.ACTIVE);
        record.setIdempotencyKey(request.getIdempotencyKey());

        // 5. Build and attach the Entries
        // Notice we use the addEntry() method to maintain the bidirectional relationship
        request.getEntries().forEach(entryRequest -> {
            RecordEntry entry = new RecordEntry();
            entry.setEntryName(entryRequest.getEntryName());
            entry.setQuantity(entryRequest.getQuantity());
            entry.setFrequency(entryRequest.getFrequency());
            entry.setDuration(entryRequest.getDuration());
            entry.setInstructions(entryRequest.getInstructions());

            record.addEntry(entry);
        });

        // 6. Save to Database (This saves BOTH the record and its entries due to CascadeType.ALL)
        Record savedRecord = recordRepository.save(record);

        // 7. Return the safe DTO response
        return RecordResponse.builder()
                .recordId(savedRecord.getId())
                .status(savedRecord.getStatus().name())
                .createdAt(LocalDateTime.now())
                .message("Record created successfully")
                .build();
    }

    @Override
    public RecordDetailsResponse getRecordById(Long id) {
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with ID: " + id));
        return mapToRecordDetailsResponse(record);
    }

    @Override
    public Page<RecordDetailsResponse> getAllRecords(Pageable pageable) {
        return recordRepository.findAll(pageable)
                .map(this::mapToRecordDetailsResponse);
    }

    private RecordDetailsResponse mapToRecordDetailsResponse(Record record) {
        List<RecordEntryResponse> entryResponses = record.getEntries() == null ? List.of() :
                record.getEntries().stream()
                        .map(entry -> RecordEntryResponse.builder()
                                .id(entry.getId())
                                .entryName(entry.getEntryName())
                                .quantity(entry.getQuantity())
                                .frequency(entry.getFrequency())
                                .duration(entry.getDuration())
                                .instructions(entry.getInstructions())
                                .build())
                        .toList();

        return RecordDetailsResponse.builder()
                .id(record.getId())
                .userId(record.getUser() != null ? record.getUser().getId() : null)
                .userName(record.getUser() != null ? record.getUser().getName() : null)
                .clientId(record.getClient() != null ? record.getClient().getId() : null)
                .clientName(record.getClient() != null ? record.getClient().getName() : null)
                .recordDate(record.getRecordDate())
                .diagnosis(record.getDiagnosis())
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .idempotencyKey(record.getIdempotencyKey())
                .entries(entryResponses)
                .build();
    }
}
