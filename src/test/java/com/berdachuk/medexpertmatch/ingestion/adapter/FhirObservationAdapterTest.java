package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.ingestion.adapter.impl.FhirObservationAdapterImpl;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FhirObservationAdapterImpl covering edge cases.
 */
class FhirObservationAdapterTest {

    private FhirObservationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FhirObservationAdapterImpl();
    }

    @Test
    @DisplayName("extractCodeText returns null for null observation")
    void extractCodeText_NullObservation() {
        assertNull(adapter.extractCodeText(null));
    }

    @Test
    @DisplayName("extractCodeText returns null when observation has no code")
    void extractCodeText_NoCode() {
        Observation observation = new Observation();
        assertNull(adapter.extractCodeText(observation));
    }

    @Test
    @DisplayName("extractCodeText returns display from coding when text not set")
    void extractCodeText_FallbackToCodingDisplay() {
        Observation observation = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setDisplay("Blood Pressure"));
        observation.setCode(code);
        assertEquals("Blood Pressure", adapter.extractCodeText(observation));
    }

    @Test
    @DisplayName("extractCodeText returns null when no text or coding display available")
    void extractCodeText_EmptyCode() {
        Observation observation = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding());
        observation.setCode(code);
        assertNull(adapter.extractCodeText(observation));
    }

    @Test
    @DisplayName("extractValueText returns null for null observation")
    void extractValueText_NullObservation() {
        assertNull(adapter.extractValueText(null));
    }

    @Test
    @DisplayName("extractValueText returns null when observation has no value")
    void extractValueText_NoValue() {
        Observation observation = new Observation();
        assertNull(adapter.extractValueText(observation));
    }

    @Test
    @DisplayName("extractValueText returns string value")
    void extractValueText_StringValue() {
        Observation observation = new Observation();
        observation.setValue(new StringType("120/80 mmHg"));
        assertEquals("120/80 mmHg", adapter.extractValueText(observation));
    }

    @Test
    @DisplayName("extractCodes returns empty list for null observation")
    void extractCodes_NullObservation() {
        assertTrue(adapter.extractCodes(null).isEmpty());
    }

    @Test
    @DisplayName("extractCodes returns empty list when no code present")
    void extractCodes_NoCode() {
        Observation observation = new Observation();
        assertTrue(adapter.extractCodes(observation).isEmpty());
    }

    @Test
    @DisplayName("extractCodes returns coding displays")
    void extractCodes_WithCodings() {
        Observation observation = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setDisplay("Systolic BP"));
        code.addCoding(new Coding().setDisplay("Diastolic BP"));
        observation.setCode(code);
        List<String> codes = adapter.extractCodes(observation);
        assertEquals(2, codes.size());
        assertTrue(codes.contains("Systolic BP"));
        assertTrue(codes.contains("Diastolic BP"));
    }

    @Test
    @DisplayName("extractCodes falls back to coding code when display not set")
    void extractCodes_FallbackToCodingCode() {
        Observation observation = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setCode("8480-6"));
        observation.setCode(code);
        List<String> codes = adapter.extractCodes(observation);
        assertEquals(1, codes.size());
        assertEquals("8480-6", codes.get(0));
    }

    @Test
    @DisplayName("extractCodes includes code text and avoids duplicates")
    void extractCodes_TextAndCodings() {
        Observation observation = new Observation();
        CodeableConcept code = new CodeableConcept();
        code.setText("Blood Pressure");
        code.addCoding(new Coding().setDisplay("Blood Pressure"));
        observation.setCode(code);
        List<String> codes = adapter.extractCodes(observation);
        assertEquals(1, codes.size());
    }
}
