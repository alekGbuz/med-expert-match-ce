package com.berdachuk.medexpertmatch.doctor.rest;

import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link MedicalSpecialtyRestController}: lookup by id, lookup by name,
 * and not-found handling.
 */
@ExtendWith(MockitoExtension.class)
class MedicalSpecialtyRestControllerTest {

    @Mock
    private MedicalSpecialtyRepository medicalSpecialtyRepository;

    @InjectMocks
    private MedicalSpecialtyRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MedicalSpecialty cardiology() {
        return new MedicalSpecialty(
                "507f1f77bcf86cd799439011",
                "Cardiology",
                "cardiology",
                "Diseases of the heart and circulatory system",
                List.of("I00-I99"),
                List.of());
    }

    @Test
    @DisplayName("GET /api/specialties/{id} returns 200 and specialty JSON when found")
    void getByIdFound() throws Exception {
        MedicalSpecialty s = cardiology();
        when(medicalSpecialtyRepository.findById(s.id())).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/specialties/{id}", s.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(s.id()))
                .andExpect(jsonPath("$.name").value("Cardiology"))
                .andExpect(jsonPath("$.normalizedName").value("cardiology"))
                .andExpect(jsonPath("$.icd10CodeRanges[0]").value("I00-I99"));
    }

    @Test
    @DisplayName("GET /api/specialties/{id} returns 404 when not found")
    void getByIdNotFound() throws Exception {
        when(medicalSpecialtyRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/specialties/{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/specialties/name/{name} returns 200 and specialty JSON when found")
    void getByNameFound() throws Exception {
        MedicalSpecialty s = cardiology();
        when(medicalSpecialtyRepository.findByName("Cardiology")).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/specialties/name/{name}", "Cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cardiology"))
                .andExpect(jsonPath("$.normalizedName").value("cardiology"));
    }

    @Test
    @DisplayName("GET /api/specialties/name/{name} returns 404 when not found")
    void getByNameNotFound() throws Exception {
        when(medicalSpecialtyRepository.findByName("Unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/specialties/name/{name}", "Unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/specialties/name/{name} handles multi-word names")
    void getByNameWithSpace() throws Exception {
        MedicalSpecialty s = new MedicalSpecialty(
                "507f1f77bcf86cd799439012",
                "Internal Medicine",
                "internal_medicine",
                "Adult primary care",
                List.of(),
                List.of());
        when(medicalSpecialtyRepository.findByName("Internal Medicine")).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/specialties/name/{name}", "Internal Medicine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Internal Medicine"));
    }
}
