package com.berdachuk.medexpertmatch.medicalcoding.rest;

import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ICD10CodeRestControllerTest {

    @Mock
    private ICD10CodeRepository icd10CodeRepository;

    @InjectMocks
    private ICD10CodeRestController controller;

    @Test
    @DisplayName("returns ICD-10 code when found by code string")
    void getByCodeReturnsCode() {
        ICD10Code code = new ICD10Code("id-1", "I21.9", "Acute myocardial infarction, unspecified", "I21", null, List.of());
        when(icd10CodeRepository.findByCode("I21.9")).thenReturn(Optional.of(code));

        var response = controller.getByCode("I21.9");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("I21.9", response.getBody().code());
    }

    @Test
    @DisplayName("throws 404 when code not found by code string")
    void getByCodeNotFound() {
        when(icd10CodeRepository.findByCode("Z99.9")).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class, () -> controller.getByCode("Z99.9"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("returns ICD-10 code when found by ID")
    void getByIdReturnsCode() {
        ICD10Code code = new ICD10Code("id-1", "I21.9", "Acute myocardial infarction", "I21", null, List.of());
        when(icd10CodeRepository.findById("id-1")).thenReturn(Optional.of(code));

        var response = controller.getById("id-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("id-1", response.getBody().id());
    }

    @Test
    @DisplayName("throws 404 when code not found by ID")
    void getByIdNotFound() {
        when(icd10CodeRepository.findById("nonexistent")).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class, () -> controller.getById("nonexistent"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
