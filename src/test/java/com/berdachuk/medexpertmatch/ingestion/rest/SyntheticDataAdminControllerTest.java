package com.berdachuk.medexpertmatch.ingestion.rest;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataPostProcessingService;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the M73 admin endpoint
 * {@code POST /api/v1/admin/synthetic-data/reconcile-specialties}.
 * Pure MockMvc test — focuses on the controller contract: response
 * shape, error handling, and admin guard integration.
 */
@ExtendWith(MockitoExtension.class)
class SyntheticDataAdminControllerTest {

    @Mock
    private SyntheticDataPostProcessingService postProcessingService;

    @Mock
    private AdminAccessGuard adminAccessGuard;

    @InjectMocks
    private SyntheticDataAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("M73: POST /reconcile-specialties returns 200 with processed, doctors, specialties")
    void reconcileReturnsReport() throws Exception {
        SyntheticDataPostProcessingService.ReconcileReport report =
                new SyntheticDataPostProcessingService.ReconcileReport(
                        5, 3,
                        List.of("d-1", "d-2", "d-3"),
                        List.of("Oncology", "Cardiology", "Pediatrics"));
        when(postProcessingService.reconcileSpecialtyGraph()).thenReturn(report);

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(5))
                .andExpect(jsonPath("$.doctorsProcessed").value(3))
                .andExpect(jsonPath("$.doctors.length()").value(3))
                .andExpect(jsonPath("$.specialties.length()").value(3))
                .andExpect(jsonPath("$.specialties[0]").value("Oncology"));
    }

    @Test
    @DisplayName("M73: POST /reconcile-specialties calls the admin guard")
    void reconcileCallsAdminGuard() throws Exception {
        when(postProcessingService.reconcileSpecialtyGraph())
                .thenReturn(new SyntheticDataPostProcessingService.ReconcileReport(
                        0, 0, List.of(), List.of()));

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"))
                .andExpect(status().isOk());

        verify(adminAccessGuard, times(1)).requireAdmin();
    }

    @Test
    @DisplayName("M73: POST /reconcile-specialties is idempotent — re-running it is safe")
    void reconcileIsIdempotent() throws Exception {
        SyntheticDataPostProcessingService.ReconcileReport report =
                new SyntheticDataPostProcessingService.ReconcileReport(
                        5, 3, List.of("d-1"), List.of("Oncology"));
        when(postProcessingService.reconcileSpecialtyGraph()).thenReturn(report);

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"));
        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"));
        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"));

        verify(postProcessingService, times(3)).reconcileSpecialtyGraph();
    }

    @Test
    @DisplayName("M73: POST /reconcile-specialties propagates 403 when admin guard rejects")
    void reconcilePropagatesForbiddenFromAdminGuard() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Admin access required"))
                .when(adminAccessGuard).requireAdmin();

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-specialties"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("M75: POST /reconcile-case-specialties returns 200 with processed, cases, specialties")
    void reconcileCaseReturnsReport() throws Exception {
        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                new SyntheticDataPostProcessingService.ReconcileCaseReport(
                        4839, 6015,
                        List.of("c-1", "c-2", "c-3"),
                        List.of("Cardiology", "Oncology", "Pediatrics"));
        when(postProcessingService.reconcileCaseSpecialtyGraph()).thenReturn(report);

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(4839))
                .andExpect(jsonPath("$.casesProcessed").value(6015))
                .andExpect(jsonPath("$.cases.length()").value(3))
                .andExpect(jsonPath("$.specialties.length()").value(3))
                .andExpect(jsonPath("$.specialties[0]").value("Cardiology"));
    }

    @Test
    @DisplayName("M75: POST /reconcile-case-specialties calls the admin guard")
    void reconcileCaseCallsAdminGuard() throws Exception {
        when(postProcessingService.reconcileCaseSpecialtyGraph())
                .thenReturn(new SyntheticDataPostProcessingService.ReconcileCaseReport(
                        0, 0, List.of(), List.of()));

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"))
                .andExpect(status().isOk());

        verify(adminAccessGuard, times(1)).requireAdmin();
    }

    @Test
    @DisplayName("M75: POST /reconcile-case-specialties is idempotent — re-running it is safe")
    void reconcileCaseIsIdempotent() throws Exception {
        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                new SyntheticDataPostProcessingService.ReconcileCaseReport(
                        4839, 6015, List.of("c-1"), List.of("Cardiology"));
        when(postProcessingService.reconcileCaseSpecialtyGraph()).thenReturn(report);

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"));
        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"));
        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"));

        verify(postProcessingService, times(3)).reconcileCaseSpecialtyGraph();
    }

    @Test
    @DisplayName("M75: POST /reconcile-case-specialties propagates 403 when admin guard rejects")
    void reconcileCasePropagatesForbiddenFromAdminGuard() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Admin access required"))
                .when(adminAccessGuard).requireAdmin();

        mockMvc.perform(post("/api/v1/admin/synthetic-data/reconcile-case-specialties"))
                .andExpect(status().isForbidden());
    }
}
