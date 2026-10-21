package com.vectoxlabs.prescription_api.controller;

import com.vectoxlabs.prescription_api.dto.CreateRecordRequest;
import com.vectoxlabs.prescription_api.dto.RecordDetailsResponse;
import com.vectoxlabs.prescription_api.dto.RecordResponse;
import com.vectoxlabs.prescription_api.exception.DuplicateRequestException;
import com.vectoxlabs.prescription_api.exception.ResourceNotFoundException;
import com.vectoxlabs.prescription_api.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RecordControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private RecordService recordService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String getValidJsonPayload(String idempotencyKey) {
        return """
            {
                "userId": 1,
                "clientId": 1,
                "recordDate": "2027-01-15T10:30:00",
                "diagnosis": "Acute pharyngitis",
                "idempotencyKey": "%s",
                "entries": [
                    {
                        "entryName": "Paracetamol 500mg",
                        "quantity": 10,
                        "frequency": "Twice daily",
                        "duration": "5 days",
                        "instructions": "Take after meals"
                    }
                ]
            }
            """.formatted(idempotencyKey);
    }

    @Test
    @DisplayName("POST /api/v1/records - 401 Unauthorized when no credentials provided")
    void testCreateRecord_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload("KEY-UNAUTH-01")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/records - 403 Forbidden when user has role STAFF (only DOCTOR/ADMIN allowed)")
    void testCreateRecord_ForbiddenForStaff() throws Exception {
        mockMvc.perform(post("/api/v1/records")
                        .with(httpBasic("staff1", "staffpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload("KEY-FORBIDDEN-01")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/records - 201 Created when authenticated as DOCTOR with valid body")
    void testCreateRecord_SuccessAsDoctor() throws Exception {
        RecordResponse response = RecordResponse.builder()
                .recordId(101L)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .message("Record created successfully")
                .build();

        when(recordService.createRecord(any(CreateRecordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/records")
                        .with(httpBasic("doctor1", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload("KEY-SUCCESS-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId").value(101))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Record created successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/records - 400 Bad Request with validation errors on missing mandatory fields")
    void testCreateRecord_ValidationFailure() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/records")
                        .with(httpBasic("doctor1", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Input validation failed"))
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.clientId").exists())
                .andExpect(jsonPath("$.validationErrors.diagnosis").exists())
                .andExpect(jsonPath("$.validationErrors.idempotencyKey").exists());
    }

    @Test
    @DisplayName("POST /api/v1/records - 409 Conflict when duplicate idempotency key is submitted")
    void testCreateRecord_DuplicateIdempotencyKey() throws Exception {
        when(recordService.createRecord(any(CreateRecordRequest.class)))
                .thenThrow(new DuplicateRequestException("A record with this idempotency key already exists."));

        mockMvc.perform(post("/api/v1/records")
                        .with(httpBasic("doctor1", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload("KEY-DUP-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A record with this idempotency key already exists."));
    }

    @Test
    @DisplayName("POST /api/v1/records - 404 Not Found when User ID or Client ID does not exist")
    void testCreateRecord_UserOrClientNotFound() throws Exception {
        when(recordService.createRecord(any(CreateRecordRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(post("/api/v1/records")
                        .with(httpBasic("doctor1", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getValidJsonPayload("KEY-NOTFOUND-01")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/records/{id} - 200 OK when record is found")
    void testGetRecordById_Success() throws Exception {
        RecordDetailsResponse response = RecordDetailsResponse.builder()
                .id(1L)
                .userId(1L)
                .userName("Dr. Emily Carter")
                .clientId(2L)
                .clientName("Alice Walker")
                .recordDate(LocalDateTime.now())
                .diagnosis("Hypertension")
                .status("ACTIVE")
                .idempotencyKey("KEY-1")
                .entries(List.of())
                .build();

        when(recordService.getRecordById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/records/1")
                        .with(httpBasic("staff1", "staffpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("Dr. Emily Carter"))
                .andExpect(jsonPath("$.clientName").value("Alice Walker"))
                .andExpect(jsonPath("$.diagnosis").value("Hypertension"));
    }

    @Test
    @DisplayName("GET /api/v1/records/{id} - 404 Not Found when record does not exist")
    void testGetRecordById_NotFound() throws Exception {
        when(recordService.getRecordById(999L))
                .thenThrow(new ResourceNotFoundException("Record not found with ID: 999"));

        mockMvc.perform(get("/api/v1/records/999")
                        .with(httpBasic("doctor1", "password123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Record not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/records - 200 OK with pagination")
    void testGetAllRecords_Paged() throws Exception {
        RecordDetailsResponse item = RecordDetailsResponse.builder()
                .id(1L)
                .userId(1L)
                .clientId(2L)
                .diagnosis("Bronchitis")
                .status("ACTIVE")
                .entries(List.of())
                .build();

        when(recordService.getAllRecords(any())).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/records?page=0&size=10")
                        .with(httpBasic("staff1", "staffpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].diagnosis").value("Bronchitis"));
    }
}
