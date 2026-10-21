package com.vectoxlabs.prescription_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordEntryResponse {
    private Long id;
    private String entryName;
    private Integer quantity;
    private String frequency;
    private String duration;
    private String instructions;
}
