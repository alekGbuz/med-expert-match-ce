package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.ingestion.adapter.impl.FhirPatientAdapterImpl;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FhirPatientAdapterImpl covering edge cases.
 */
class FhirPatientAdapterTest {

    private FhirPatientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FhirPatientAdapterImpl();
    }

    @Test
    @DisplayName("extractAge returns null for null patient")
    void extractAge_NullPatient() {
        assertNull(adapter.extractAge(null));
    }

    @Test
    @DisplayName("extractAge returns null when patient has no birth date")
    void extractAge_NoBirthDate() {
        Patient patient = new Patient();
        assertNull(adapter.extractAge(patient));
    }

    @Test
    @DisplayName("extractAge returns null for null birth date value")
    void extractAge_NullBirthDate() {
        Patient patient = new Patient();
        patient.setBirthDateElement(new org.hl7.fhir.r5.model.DateType());
        assertNull(adapter.extractAge(patient));
    }

    @Test
    @DisplayName("extractAge returns null for future birth date (negative age)")
    void extractAge_FutureBirthDate() {
        Patient patient = new Patient();
        LocalDate futureDate = LocalDate.now().plusYears(10);
        patient.setBirthDate(Date.from(futureDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        assertNull(adapter.extractAge(patient));
    }

    @Test
    @DisplayName("extractAge returns null for birth date resulting in age > 120")
    void extractAge_TooOld() {
        Patient patient = new Patient();
        LocalDate veryOldDate = LocalDate.now().minusYears(150);
        patient.setBirthDate(Date.from(veryOldDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        assertNull(adapter.extractAge(patient));
    }

    @Test
    @DisplayName("extractAge calculates correct age for newborn")
    void extractAge_Newborn() {
        Patient patient = new Patient();
        LocalDate today = LocalDate.now();
        patient.setBirthDate(Date.from(today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        Integer age = adapter.extractAge(patient);
        assertNotNull(age);
        assertEquals(0, age.intValue());
    }

    @Test
    @DisplayName("isAnonymized returns false for null patient")
    void isAnonymized_NullPatient() {
        assertFalse(adapter.isAnonymized(null));
    }

    @Test
    @DisplayName("isAnonymized returns false when patient has identifiers")
    void isAnonymized_WithIdentifiers() {
        Patient patient = new Patient();
        patient.addIdentifier().setValue("test-id");
        assertFalse(adapter.isAnonymized(patient));
    }

    @Test
    @DisplayName("isAnonymized returns false when patient has addresses")
    void isAnonymized_WithAddresses() {
        Patient patient = new Patient();
        patient.addAddress().setCity("Test City");
        assertFalse(adapter.isAnonymized(patient));
    }

    @Test
    @DisplayName("isAnonymized returns false when patient has telecom")
    void isAnonymized_WithTelecom() {
        Patient patient = new Patient();
        patient.addTelecom().setValue("+1234567890");
        assertFalse(adapter.isAnonymized(patient));
    }

    @Test
    @DisplayName("isAnonymized returns true for empty patient")
    void isAnonymized_EmptyPatient() {
        Patient patient = new Patient();
        assertTrue(adapter.isAnonymized(patient));
    }
}
