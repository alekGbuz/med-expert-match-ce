package com.berdachuk.medexpertmatch.medicalcase.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalCaseTest {

    @Test
    @DisplayName("record construction with all fields")
    void recordConstruction() {
        MedicalCase mc = new MedicalCase(
                "case-001", 45, "Chest pain", "Shortness of breath",
                "Acute MI", List.of("I21.9"), List.of("22298006"),
                UrgencyLevel.HIGH, "Cardiology", CaseType.INPATIENT,
                "Patient with history of CAD", "Abstract text here",
                new BigDecimal("42.3601"), new BigDecimal("-71.0589"));

        assertEquals("case-001", mc.id());
        assertEquals(45, mc.patientAge());
        assertEquals("Chest pain", mc.chiefComplaint());
        assertEquals(UrgencyLevel.HIGH, mc.urgencyLevel());
        assertEquals("Cardiology", mc.requiredSpecialty());
        assertEquals(CaseType.INPATIENT, mc.caseType());
        assertEquals(List.of("I21.9"), mc.icd10Codes());
        assertEquals(new BigDecimal("42.3601"), mc.locationLatitude());
    }

    @Test
    @DisplayName("compact constructor defaults coordinates to null")
    void compactConstructorDefaultsCoordinates() {
        MedicalCase mc = new MedicalCase(
                "case-002", 30, "Headache", "Nausea",
                "Migraine", List.of("G43.9"), List.of(),
                UrgencyLevel.MEDIUM, "Neurology", CaseType.CONSULT_REQUEST,
                "Recurrent migraines", "Abstract");

        assertNull(mc.locationLatitude());
        assertNull(mc.locationLongitude());
    }

    @Test
    @DisplayName("record with null optional fields")
    void recordWithNullOptionals() {
        MedicalCase mc = new MedicalCase(
                "case-003", null, null, null,
                null, List.of(), List.of(),
                null, null, null,
                null, null);

        assertNull(mc.patientAge());
        assertNull(mc.chiefComplaint());
        assertNull(mc.urgencyLevel());
        assertNull(mc.requiredSpecialty());
    }
}
