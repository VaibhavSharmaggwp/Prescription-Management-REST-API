package com.vectoxlabs.prescription_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordEntryRequest {
    @NotBlank(message = "Entry name is mandatory")
    private String entryName;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Frequency is mandatory (e.g., 'Twice a day')")
    private String frequency;

    @NotBlank(message = "Duration is mandatory (e.g., '7 days')")
    private String duration;

    // Instructions are optional according to DOC
    private String instructions;

}
