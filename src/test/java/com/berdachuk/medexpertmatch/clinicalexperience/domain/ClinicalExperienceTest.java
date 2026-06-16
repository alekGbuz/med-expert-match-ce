package com.berdachuk.medexpertmatch.clinicalexperience.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalExperienceTest {

    @Test
    @DisplayName("record construction with all fields")
    void recordConstruction() {
        ClinicalExperience exp = new ClinicalExperience(
                "exp-001", "doc-001", "case-001",
                List.of("PCI", "Stent"),
                "HIGH", "SUCCESS",
                List.of(), 14, 5);

        assertEquals("exp-001", exp.id());
        assertEquals("doc-001", exp.doctorId());
        assertEquals("case-001", exp.caseId());
        assertEquals(List.of("PCI", "Stent"), exp.proceduresPerformed());
        assertEquals("HIGH", exp.complexityLevel());
        assertEquals("SUCCESS", exp.outcome());
        assertTrue(exp.complications().isEmpty());
        assertEquals(14, exp.timeToResolution());
        assertEquals(5, exp.rating());
    }

    @Test
    @DisplayName("record with complications")
    void recordWithComplications() {
        ClinicalExperience exp = new ClinicalExperience(
                "exp-002", "doc-002", "case-002",
                List.of("Surgery"),
                "CRITICAL", "COMPLICATED",
                List.of("Infection", "Bleeding"), 30, 2);

        assertEquals(2, exp.complications().size());
        assertEquals("COMPLICATED", exp.outcome());
        assertEquals(2, exp.rating());
    }
}
