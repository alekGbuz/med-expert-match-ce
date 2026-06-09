package com.berdachuk.medexpertmatch.doctor.rest;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
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
 * Unit tests for {@link DoctorRestController}. Pure MockMvc test (no Spring context,
 * no Testcontainers) — focuses on controller contract: lookup, not-found handling,
 * and JSON serialization shape.
 */
@ExtendWith(MockitoExtension.class)
class DoctorRestControllerTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/doctors/{id} returns 200 and doctor JSON when found")
    void getByIdFound() throws Exception {
        Doctor d = new Doctor(
                "doc-1",
                "Dr. Smith",
                "smith@example.com",
                List.of("cardiology"),
                List.of("ABIM"),
                List.of("fac-1"),
                true,
                "AVAILABLE");
        when(doctorRepository.findById("doc-1")).thenReturn(Optional.of(d));

        mockMvc.perform(get("/api/doctors/{id}", "doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.name").value("Dr. Smith"))
                .andExpect(jsonPath("$.email").value("smith@example.com"))
                .andExpect(jsonPath("$.specialties[0]").value("cardiology"))
                .andExpect(jsonPath("$.telehealthEnabled").value(true))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/doctors/{id} returns 404 when not found")
    void getByIdNotFound() throws Exception {
        when(doctorRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/doctors/{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/doctors/{id} supports 19-digit numeric IDs")
    void getByIdAcceptsNumericId() throws Exception {
        String numericId = "8760000000000420950";
        Doctor d = new Doctor(numericId, "Dr. Numeric", "n@example.com",
                List.of(), List.of(), List.of(), false, "BUSY");
        when(doctorRepository.findById(numericId)).thenReturn(Optional.of(d));

        mockMvc.perform(get("/api/doctors/{id}", numericId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(numericId))
                .andExpect(jsonPath("$.telehealthEnabled").value(false));
    }

    @Test
    @DisplayName("GET /api/doctors/{id} supports UUID-style IDs")
    void getByIdAcceptsUuidId() throws Exception {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        Doctor d = new Doctor(uuid, "Dr. UUID", "u@example.com",
                List.of("oncology"), List.of(), List.of(), true, "AVAILABLE");
        when(doctorRepository.findById(uuid)).thenReturn(Optional.of(d));

        mockMvc.perform(get("/api/doctors/{id}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid));
    }
}
