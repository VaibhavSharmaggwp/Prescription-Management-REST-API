package com.vectoxlabs.prescription_api.service;

import com.vectoxlabs.prescription_api.dto.CreateRecordRequest;
import com.vectoxlabs.prescription_api.dto.RecordDetailsResponse;
import com.vectoxlabs.prescription_api.dto.RecordEntryRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private RecordServiceImpl recordService;

    private User sampleUser;
    private Client sampleClient;
    private CreateRecordRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Dr. Emily Carter");
        sampleUser.setRegistrationNumber("REG-8821");
        sampleUser.setPhoneNumber("555-0144");

        sampleClient = new Client();
        sampleClient.setId(2L);
        sampleClient.setName("Alice Walker");
        sampleClient.setDateOfBirth(LocalDate.of(1992, 4, 12));
        sampleClient.setPhoneNumber("555-0199");

        RecordEntryRequest entryRequest = new RecordEntryRequest();
        entryRequest.setEntryName("Amoxicillin 500mg");
        entryRequest.setQuantity(20);
        entryRequest.setFrequency("Three times daily");
        entryRequest.setDuration("7 days");
        entryRequest.setInstructions("Take after meals with water");

        sampleRequest = new CreateRecordRequest();
        sampleRequest.setUserId(1L);
        sampleRequest.setClientId(2L);
        sampleRequest.setRecordDate(LocalDateTime.now().plusDays(1));
        sampleRequest.setDiagnosis("Bacterial Sinusitis");
        sampleRequest.setIdempotencyKey("IDEMP-KEY-TEST-001");
        sampleRequest.setEntries(List.of(entryRequest));
    }

    @Test
    @DisplayName("createRecord - Success: builds entity, attaches entries, saves atomically, returns response")
    void testCreateRecord_Success() {
        // Arrange
        when(recordRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(sampleClient));

        when(recordRepository.save(any(Record.class))).thenAnswer(invocation -> {
            Record toSave = invocation.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        // Act
        RecordResponse response = recordService.createRecord(sampleRequest);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getRecordId());
        assertEquals(RecordStatus.ACTIVE.name(), response.getStatus());
        assertEquals("Record created successfully", response.getMessage());
        assertNotNull(response.getCreatedAt());

        verify(recordRepository, times(1)).findByIdempotencyKey(sampleRequest.getIdempotencyKey());
        verify(userRepository, times(1)).findById(1L);
        verify(clientRepository, times(1)).findById(2L);
        verify(recordRepository, times(1)).save(any(Record.class));
    }

    @Test
    @DisplayName("createRecord - Throws DuplicateRequestException when idempotency key already exists")
    void testCreateRecord_DuplicateIdempotencyKey() {
        // Arrange
        when(recordRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                .thenReturn(Optional.of(new Record()));

        // Act & Assert
        DuplicateRequestException ex = assertThrows(DuplicateRequestException.class, () ->
                recordService.createRecord(sampleRequest)
        );
        assertEquals("A record with this idempotency key already exists.", ex.getMessage());

        verify(recordRepository, times(1)).findByIdempotencyKey(sampleRequest.getIdempotencyKey());
        verify(userRepository, never()).findById(any());
        verify(clientRepository, never()).findById(any());
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRecord - Throws ResourceNotFoundException when user is not found")
    void testCreateRecord_UserNotFound() {
        // Arrange
        when(recordRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                recordService.createRecord(sampleRequest)
        );
        assertTrue(ex.getMessage().contains("User not found with ID: 1"));

        verify(clientRepository, never()).findById(any());
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRecord - Throws ResourceNotFoundException when client is not found")
    void testCreateRecord_ClientNotFound() {
        // Arrange
        when(recordRepository.findByIdempotencyKey(sampleRequest.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(clientRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                recordService.createRecord(sampleRequest)
        );
        assertTrue(ex.getMessage().contains("Client not found with ID: 2"));

        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("getRecordById - Success: returns full details including user, client, and entries")
    void testGetRecordById_Success() {
        // Arrange
        Record record = new Record();
        record.setId(50L);
        record.setUser(sampleUser);
        record.setClient(sampleClient);
        record.setRecordDate(LocalDateTime.of(2026, 10, 20, 10, 0));
        record.setDiagnosis("Hypertension Stage 1");
        record.setStatus(RecordStatus.ACTIVE);
        record.setIdempotencyKey("KEY-50");

        RecordEntry entry = new RecordEntry();
        entry.setId(501L);
        entry.setEntryName("Lisinopril 10mg");
        entry.setQuantity(30);
        entry.setFrequency("Once daily");
        entry.setDuration("30 days");
        entry.setInstructions("Take in the morning");
        record.addEntry(entry);

        when(recordRepository.findById(50L)).thenReturn(Optional.of(record));

        // Act
        RecordDetailsResponse response = recordService.getRecordById(50L);

        // Assert
        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Dr. Emily Carter", response.getUserName());
        assertEquals(2L, response.getClientId());
        assertEquals("Alice Walker", response.getClientName());
        assertEquals("Hypertension Stage 1", response.getDiagnosis());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, response.getEntries().size());
        assertEquals("Lisinopril 10mg", response.getEntries().get(0).getEntryName());
        assertEquals("Take in the morning", response.getEntries().get(0).getInstructions());

        verify(recordRepository, times(1)).findById(50L);
    }

    @Test
    @DisplayName("getRecordById - Throws ResourceNotFoundException when record ID does not exist")
    void testGetRecordById_NotFound() {
        // Arrange
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                recordService.getRecordById(999L)
        );
        assertTrue(ex.getMessage().contains("Record not found with ID: 999"));

        verify(recordRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getAllRecords - Returns paginated list of record details")
    void testGetAllRecords_Success() {
        // Arrange
        Record record = new Record();
        record.setId(1L);
        record.setUser(sampleUser);
        record.setClient(sampleClient);
        record.setRecordDate(LocalDateTime.now());
        record.setDiagnosis("Common Cold");
        record.setStatus(RecordStatus.ACTIVE);
        record.setIdempotencyKey("KEY-PAGED-1");

        Pageable pageable = PageRequest.of(0, 5);
        Page<Record> recordPage = new PageImpl<>(List.of(record), pageable, 1);

        when(recordRepository.findAll(pageable)).thenReturn(recordPage);

        // Act
        Page<RecordDetailsResponse> result = recordService.getAllRecords(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Common Cold", result.getContent().get(0).getDiagnosis());

        verify(recordRepository, times(1)).findAll(pageable);
    }
}
