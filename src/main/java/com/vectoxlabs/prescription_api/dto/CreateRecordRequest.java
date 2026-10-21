package com.vectoxlabs.prescription_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateRecordRequest {
    @NotNull(message = "User ID is mandatory")
    private Long userId;

    @NotNull(message = "Client ID is mandatory")
    private Long clientId;

    @NotNull(message = "Record date is mandatory")
    @FutureOrPresent(message = "Record date cannot be in the past")
    private LocalDateTime recordDate;

    @NotNull(message = "Diagnosis is mandatory")
    private String diagnosis;

    @NotEmpty(message = "A record must contain at least one entry")
    @Valid  // This tells Spring to also run the validations inside RecordEntryRequest
    private List<RecordEntryRequest> entries;

    // To satisfy the "Prevent accidental duplicate submissions" requirement,
    // we require the client to send a unique request ID.

    @NotNull(message = "Idempotency key is mandatory to prevent duplicate submissions")
    private String idempotencyKey;

}
