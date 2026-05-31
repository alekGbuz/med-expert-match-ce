package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.harness.impl.AgentResponseVerifierImpl;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentResponseVerifierTest {

    private final AgentResponseVerifier verifier = new AgentResponseVerifierImpl();

    @Test
    @DisplayName("passes when matches meet minimum and have doctor names")
    void passesValidMatches() {
        Doctor doctor = new Doctor("d1", "Dr. Smith", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 85.0, 1, "Strong specialty fit");
        VerificationResult result = verifier.verify(new VerificationRequest(
                HarnessWorkflowType.DOCTOR_MATCH,
                "6a1c68963a08e800010de68e",
                List.of(match),
                1));
        assertTrue(result.passed());
    }

    @Test
    @DisplayName("fails when match list is empty and min matches is 1")
    void failsEmptyMatches() {
        VerificationResult result = verifier.verify(new VerificationRequest(
                HarnessWorkflowType.DOCTOR_MATCH,
                "6a1c68963a08e800010de68e",
                List.of(),
                1));
        assertFalse(result.passed());
        assertEquals(HarnessFailureReason.TOOL_OUTPUT_INVALID, result.reasonCode());
    }
}
