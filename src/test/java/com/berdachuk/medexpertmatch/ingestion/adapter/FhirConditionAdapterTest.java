package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.ingestion.adapter.impl.FhirConditionAdapterImpl;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FhirConditionAdapterImpl covering edge cases.
 */
class FhirConditionAdapterTest {

    private FhirConditionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FhirConditionAdapterImpl();
    }

    @Test
    @DisplayName("extractIcd10Codes returns empty list for null condition")
    void extractIcd10Codes_NullCondition() {
        assertTrue(adapter.extractIcd10Codes(null).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes returns empty list when condition has no code")
    void extractIcd10Codes_NoCode() {
        Condition condition = new Condition();
        assertTrue(adapter.extractIcd10Codes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes returns empty list when code has no coding")
    void extractIcd10Codes_NoCoding() {
        Condition condition = new Condition();
        condition.setCode(new CodeableConcept());
        assertTrue(adapter.extractIcd10Codes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes returns empty list when coding has null code value")
    void extractIcd10Codes_NullCodeValue() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode((String) null));
        condition.setCode(code);
        assertTrue(adapter.extractIcd10Codes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes works with empty code value")
    void extractIcd10Codes_EmptyCodeValue() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode(""));
        condition.setCode(code);
        assertTrue(adapter.extractIcd10Codes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes filters non-ICD10 systems")
    void extractIcd10Codes_NonIcd10System() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-9")
                .setCode("401.9"));
        condition.setCode(code);
        assertTrue(adapter.extractIcd10Codes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractIcd10Codes extracts only ICD-10 from mixed codes")
    void extractIcd10Codes_MixedSystems() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9"));
        code.addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("22298006"));
        condition.setCode(code);
        List<String> codes = adapter.extractIcd10Codes(condition);
        assertEquals(1, codes.size());
        assertEquals("I21.9", codes.get(0));
    }

    @Test
    @DisplayName("extractSnomedCodes returns empty list for null condition")
    void extractSnomedCodes_NullCondition() {
        assertTrue(adapter.extractSnomedCodes(null).isEmpty());
    }

    @Test
    @DisplayName("extractSnomedCodes returns empty list when condition has no code")
    void extractSnomedCodes_NoCode() {
        Condition condition = new Condition();
        assertTrue(adapter.extractSnomedCodes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractSnomedCodes filters non-SNOMED systems")
    void extractSnomedCodes_NonSnomedSystem() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9"));
        condition.setCode(code);
        assertTrue(adapter.extractSnomedCodes(condition).isEmpty());
    }

    @Test
    @DisplayName("extractCodeText returns null for null condition")
    void extractCodeText_NullCondition() {
        assertNull(adapter.extractCodeText(null));
    }

    @Test
    @DisplayName("extractCodeText returns null when condition has no code")
    void extractCodeText_NoCode() {
        Condition condition = new Condition();
        assertNull(adapter.extractCodeText(condition));
    }

    @Test
    @DisplayName("extractCodeText returns display text from first coding when text not set")
    void extractCodeText_FallbackToCodingDisplay() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9")
                .setDisplay("Acute myocardial infarction"));
        condition.setCode(code);
        assertEquals("Acute myocardial infarction", adapter.extractCodeText(condition));
    }

    @Test
    @DisplayName("extractSeverity returns null for null condition")
    void extractSeverity_NullCondition() {
        assertNull(adapter.extractSeverity(null));
    }

    @Test
    @DisplayName("extractSeverity returns null when no severity present")
    void extractSeverity_NoSeverity() {
        Condition condition = new Condition();
        assertNull(adapter.extractSeverity(condition));
    }

    @Test
    @DisplayName("extractSeverity returns severity text")
    void extractSeverity_Text() {
        Condition condition = new Condition();
        CodeableConcept severity = new CodeableConcept();
        severity.setText("severe");
        condition.setSeverity(severity);
        assertEquals("severe", adapter.extractSeverity(condition));
    }

    @Test
    @DisplayName("extractSeverity returns display from coding when text not set")
    void extractSeverity_FallbackToCodingDisplay() {
        Condition condition = new Condition();
        CodeableConcept severity = new CodeableConcept();
        severity.addCoding(new Coding().setDisplay("Severe"));
        condition.setSeverity(severity);
        assertEquals("Severe", adapter.extractSeverity(condition));
    }

    @Test
    @DisplayName("extractSeverity returns code from coding when display not set")
    void extractSeverity_FallbackToCodingCode() {
        Condition condition = new Condition();
        CodeableConcept severity = new CodeableConcept();
        severity.addCoding(new Coding().setCode("24484000"));
        condition.setSeverity(severity);
        assertEquals("24484000", adapter.extractSeverity(condition));
    }

    @Test
    @DisplayName("extractClinicalStatus returns null for null condition")
    void extractClinicalStatus_NullCondition() {
        assertNull(adapter.extractClinicalStatus(null));
    }

    @Test
    @DisplayName("extractClinicalStatus returns null when no clinical status present")
    void extractClinicalStatus_NoStatus() {
        Condition condition = new Condition();
        assertNull(adapter.extractClinicalStatus(condition));
    }

    @Test
    @DisplayName("extractClinicalStatus returns status code")
    void extractClinicalStatus_Active() {
        Condition condition = new Condition();
        CodeableConcept status = new CodeableConcept();
        status.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active"));
        condition.setClinicalStatus(status);
        assertEquals("active", adapter.extractClinicalStatus(condition));
    }
}
