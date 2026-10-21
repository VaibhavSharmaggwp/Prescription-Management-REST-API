package com.vectoxlabs.prescription_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDetailsResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long clientId;
    private String clientName;
    private LocalDateTime recordDate;
    private String diagnosis;
    private String status;
    private String idempotencyKey;
    private List<RecordEntryResponse> entries;
}
