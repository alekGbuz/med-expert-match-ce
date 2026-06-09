package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.ingestion.adapter.impl.FhirEncounterAdapterImpl;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Encounter;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for FhirEncounterAdapterImpl covering edge cases.
 */
class FhirEncounterAdapterTest {

    private FhirEncounterAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FhirEncounterAdapterImpl();
    }

    @Test
    @DisplayName("extractType returns null for null encounter")
    void extractType_NullEncounter() {
        assertNull(adapter.extractType(null));
    }

    @Test
    @DisplayName("extractType returns null when no type present")
    void extractType_NoType() {
        Encounter encounter = new Encounter();
        assertNull(adapter.extractType(encounter));
    }

    @Test
    @DisplayName("extractType returns type text")
    void extractType_Text() {
        Encounter encounter = new Encounter();
        encounter.addType().setText("Emergency Department Visit");
        assertEquals("Emergency Department Visit", adapter.extractType(encounter));
    }

    @Test
    @DisplayName("extractType returns display from coding when text not set")
    void extractType_FallbackToCodingDisplay() {
        Encounter encounter = new Encounter();
        CodeableConcept type = new CodeableConcept();
        type.addCoding(new Coding().setDisplay("Emergency"));
        encounter.addType(type);
        assertEquals("Emergency", adapter.extractType(encounter));
    }

    @Test
    @DisplayName("extractType returns code from coding when display not set")
    void extractType_FallbackToCodingCode() {
        Encounter encounter = new Encounter();
        CodeableConcept type = new CodeableConcept();
        type.addCoding(new Coding().setCode("EM"));
        encounter.addType(type);
        assertEquals("EM", adapter.extractType(encounter));
    }

    @Test
    @DisplayName("extractStatus returns null for null encounter")
    void extractStatus_NullEncounter() {
        assertNull(adapter.extractStatus(null));
    }

    @Test
    @DisplayName("extractStatus returns null when no status present")
    void extractStatus_NoStatus() {
        Encounter encounter = new Encounter();
        assertNull(adapter.extractStatus(encounter));
    }

    @Test
    @DisplayName("extractClass returns null for null encounter")
    void extractClass_NullEncounter() {
        assertNull(adapter.extractClass(null));
    }

    @Test
    @DisplayName("extractClass returns null when no class present")
    void extractClass_NoClass() {
        Encounter encounter = new Encounter();
        assertNull(adapter.extractClass(encounter));
    }

    @Test
    @DisplayName("extractClass returns IMP for inpatient encounter")
    void extractClass_Inpatient() {
        Encounter encounter = new Encounter();
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("IMP"));
        encounter.addClass_(classConcept);
        assertEquals("IMP", adapter.extractClass(encounter));
    }

    @Test
    @DisplayName("extractClass returns AMB for ambulatory encounter")
    void extractClass_Ambulatory() {
        Encounter encounter = new Encounter();
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB"));
        encounter.addClass_(classConcept);
        assertEquals("AMB", adapter.extractClass(encounter));
    }

    @Test
    @DisplayName("extractServiceProvider returns null for null encounter")
    void extractServiceProvider_NullEncounter() {
        assertNull(adapter.extractServiceProvider(null));
    }

    @Test
    @DisplayName("extractServiceProvider returns null when no service provider present")
    void extractServiceProvider_NoProvider() {
        Encounter encounter = new Encounter();
        assertNull(adapter.extractServiceProvider(encounter));
    }

    @Test
    @DisplayName("extractServiceProvider extracts ID from reference")
    void extractServiceProvider_WithReference() {
        Encounter encounter = new Encounter();
        encounter.setServiceProvider(new Reference("Organization/123"));
        assertEquals("123", adapter.extractServiceProvider(encounter));
    }

    @Test
    @DisplayName("extractServiceProvider returns reference when no slash")
    void extractServiceProvider_NoSlash() {
        Encounter encounter = new Encounter();
        encounter.setServiceProvider(new Reference("123"));
        assertEquals("123", adapter.extractServiceProvider(encounter));
    }

    @Test
    @DisplayName("extractServiceProvider returns null for empty reference")
    void extractServiceProvider_EmptyReference() {
        Encounter encounter = new Encounter();
        encounter.setServiceProvider(new Reference(""));
        assertNull(adapter.extractServiceProvider(encounter));
    }
}
