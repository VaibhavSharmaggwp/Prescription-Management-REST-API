package com.vectoxlabs.prescription_api.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecordResponse {
    private Long recordId;
    private String status;
    private LocalDateTime createdAt;
    private String message;
}
