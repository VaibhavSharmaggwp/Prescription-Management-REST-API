package com.vectoxlabs.prescription_api.controller;


import com.vectoxlabs.prescription_api.dto.CreateRecordRequest;
import com.vectoxlabs.prescription_api.dto.RecordDetailsResponse;
import com.vectoxlabs.prescription_api.dto.RecordResponse;
import com.vectoxlabs.prescription_api.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService recordService;

    // @Valid is CRITICAL here. It tells Spring to execute the @NotNull, @Min, etc.
    // annotations inside  DTO before the code even enters this method.
    @PostMapping
    public ResponseEntity<RecordResponse> createRecord(@Valid @RequestBody CreateRecordRequest request){
        RecordResponse response = recordService.createRecord(request);

        // return 201 CREATED for a successful POST, not 200 OK.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordDetailsResponse> getRecordById(@PathVariable Long id){
        RecordDetailsResponse response = recordService.getRecordById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<RecordDetailsResponse>> getAllRecords(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable){
        Page<RecordDetailsResponse> records = recordService.getAllRecords(pageable);
        return ResponseEntity.ok(records);
    }
}
