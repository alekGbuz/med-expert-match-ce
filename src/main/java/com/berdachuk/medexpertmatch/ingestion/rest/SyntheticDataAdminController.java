package com.berdachuk.medexpertmatch.ingestion.rest;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataPostProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for synthetic data housekeeping (M73).
 * <p>
 * Lives at {@code /api/v1/admin/synthetic-data/...} so all admin-only
 * synthetic-data operations share the same admin surface and the
 * {@link AdminAccessGuard} applies uniformly.
 */
@Tag(name = "Admin · Synthetic Data", description = "Admin synthetic-data operations (requires X-User-Id: admin)")
@RestController
@RequestMapping("/api/v1/admin/synthetic-data")
@RequiredArgsConstructor
public class SyntheticDataAdminController {

    private final SyntheticDataPostProcessingService postProcessingService;
    private final AdminAccessGuard adminAccessGuard;

    /**
     * M73: re-walks the SQL doctor table and ensures every
     * {@code (d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)}
     * edge implied by {@code d.specialties} exists in the graph.
     * Idempotent; safe to re-run.
     * <p>
     * Triggers a one-shot heal of gaps like the Kory Terry bug
     * (Oncology in SQL but no graph edge) without re-running the
     * whole synthetic-data generation.
     */
    @Operation(
            summary = "Reconcile doctor–specialty edges in the graph",
            description = "Walks the SQL doctor table and (re)creates every "
                    + "(Doctor)-[:SPECIALIZES_IN]->(MedicalSpecialty) edge in the graph. "
                    + "Idempotent; safe to re-run."
    )
    @PostMapping("/reconcile-specialties")
    public SyntheticDataPostProcessingService.ReconcileReport reconcileSpecialties() {
        adminAccessGuard.requireAdmin();
        return postProcessingService.reconcileSpecialtyGraph();
    }

    /**
     * M75: re-walks the SQL medical_cases table and ensures every case
     * with a non-blank {@code required_specialty} has a
     * {@code (c:MedicalCase)-[:REQUIRES_SPECIALTY]->(s:MedicalSpecialty)}
     * edge in the graph. Idempotent; safe to re-run.
     * <p>
     * Triggers a one-shot heal of the case-side gap without re-running
     * the whole synthetic-data generation. Mirror of the M73
     * {@code /reconcile-specialties} endpoint but for the case side.
     */
    @Operation(
            summary = "Reconcile case–specialty edges in the graph",
            description = "Walks the SQL medical_cases table and (re)creates every "
                    + "(MedicalCase)-[:REQUIRES_SPECIALTY]->(MedicalSpecialty) edge for "
                    + "cases with a non-blank required_specialty. Idempotent; safe to re-run."
    )
    @PostMapping("/reconcile-case-specialties")
    public SyntheticDataPostProcessingService.ReconcileCaseReport reconcileCaseSpecialties() {
        adminAccessGuard.requireAdmin();
        return postProcessingService.reconcileCaseSpecialtyGraph();
    }
}
