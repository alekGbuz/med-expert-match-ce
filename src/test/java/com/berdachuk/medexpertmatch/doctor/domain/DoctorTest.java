package com.berdachuk.medexpertmatch.doctor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoctorTest {

    @Test
    @DisplayName("record construction with all fields")
    void recordConstruction() {
        Doctor doctor = new Doctor(
                "doc-001", "Dr. Smith", "smith@hospital.com",
                List.of("Cardiology", "Internal Medicine"),
                List.of("ABIM", "FACC"),
                List.of("fac-001", "fac-002"),
                true, "AVAILABLE");

        assertEquals("doc-001", doctor.id());
        assertEquals("Dr. Smith", doctor.name());
        assertEquals("smith@hospital.com", doctor.email());
        assertEquals(2, doctor.specialties().size());
        assertEquals(2, doctor.certifications().size());
        assertEquals(2, doctor.facilityIds().size());
        assertTrue(doctor.telehealthEnabled());
        assertEquals("AVAILABLE", doctor.availabilityStatus());
    }

    @Test
    @DisplayName("doctor with no certifications or facilities")
    void minimalDoctor() {
        Doctor doctor = new Doctor(
                "doc-002", "Dr. Jones", null,
                List.of("Neurology"),
                List.of(), List.of(),
                false, "BUSY");

        assertNull(doctor.email());
        assertTrue(doctor.certifications().isEmpty());
        assertTrue(doctor.facilityIds().isEmpty());
        assertFalse(doctor.telehealthEnabled());
    }
}
