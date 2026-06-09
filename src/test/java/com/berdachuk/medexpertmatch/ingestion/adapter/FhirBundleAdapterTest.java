package com.berdachuk.medexpertmatch.ingestion.adapter;

import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.ingestion.adapter.impl.*;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.hl7.fhir.r5.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FhirBundleAdapterImpl.
 */
@ExtendWith(MockitoExtension.class)
class FhirBundleAdapterTest {

    private FhirBundleAdapter adapter;

    @Mock
    private MedicalSpecialtyRepository medicalSpecialtyRepository;

    @BeforeEach
    void setUp() {
        var patientAdapter = new FhirPatientAdapterImpl();
        var conditionAdapter = new FhirConditionAdapterImpl();
        var encounterAdapter = new FhirEncounterAdapterImpl();
        var observationAdapter = new FhirObservationAdapterImpl();
        adapter = new FhirBundleAdapterImpl(
                patientAdapter, conditionAdapter, encounterAdapter, observationAdapter, medicalSpecialtyRepository);
    }

    @Test
    @DisplayName("isValidBundle returns false for null")
    void isValidBundle_Null() {
        assertFalse(adapter.isValidBundle(null));
    }

    @Test
    @DisplayName("isValidBundle returns false for empty bundle")
    void isValidBundle_Empty() {
        Bundle bundle = new Bundle();
        assertFalse(adapter.isValidBundle(bundle));
    }

    @Test
    @DisplayName("isValidBundle returns false when entries have no resources")
    void isValidBundle_EntriesWithoutResources() {
        Bundle bundle = new Bundle();
        bundle.addEntry(new Bundle.BundleEntryComponent());
        assertFalse(adapter.isValidBundle(bundle));
    }

    @Test
    @DisplayName("isValidBundle returns true for bundle with resource")
    void isValidBundle_WithResource() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        assertTrue(adapter.isValidBundle(bundle));
    }

    @Test
    @DisplayName("isValidBundle returns true for bundle with multiple entries, at least one has resource")
    void isValidBundle_MultipleEntries() {
        Bundle bundle = new Bundle();
        bundle.addEntry(new Bundle.BundleEntryComponent());
        bundle.addEntry().setResource(new Patient());
        assertTrue(adapter.isValidBundle(bundle));
    }

    @Test
    @DisplayName("convertBundleToMedicalCase throws IllegalArgumentException for invalid bundle")
    void convertBundle_InvalidBundle() {
        assertThrows(IllegalArgumentException.class, () -> adapter.convertBundleToMedicalCase(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.convertBundleToMedicalCase(new Bundle()));
    }

    @Test
    @DisplayName("convertBundleToMedicalCase converts bundle with patient only (minimal)")
    void convertBundle_MinimalBundle() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertNotNull(medicalCase);
        assertNull(medicalCase.patientAge());
        assertTrue(medicalCase.icd10Codes().isEmpty());
        assertTrue(medicalCase.snomedCodes().isEmpty());
        assertEquals(UrgencyLevel.MEDIUM, medicalCase.urgencyLevel());
        assertEquals(CaseType.CONSULT_REQUEST, medicalCase.caseType());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase determines CRITICAL urgency for emergency encounter")
    void convertBundle_CriticalUrgency() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9")
                .setDisplay("Acute myocardial infarction"));
        condition.setCode(code);
        condition.setClinicalStatus(new CodeableConcept().addCoding(
                new Coding().setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")));

        Encounter encounter = new Encounter();
        encounter.setStatus(Enumerations.EncounterStatus.INPROGRESS);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("EMR")
                .setDisplay("emergency"));
        encounter.addClass_(classConcept);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(encounter);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals(UrgencyLevel.CRITICAL, medicalCase.urgencyLevel());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase determines HIGH urgency for inpatient encounter")
    void convertBundle_HighUrgency() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I50.9"));
        condition.setCode(code);

        Encounter encounter = new Encounter();
        encounter.setStatus(Enumerations.EncounterStatus.INPROGRESS);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("IMP")
                .setDisplay("inpatient encounter"));
        encounter.addClass_(classConcept);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(encounter);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals(UrgencyLevel.HIGH, medicalCase.urgencyLevel());
        assertEquals(CaseType.INPATIENT, medicalCase.caseType());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase handles observation entries")
    void convertBundle_WithObservations() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(30);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11.9")
                .setDisplay("Type 2 diabetes"));
        condition.setCode(code);

        Observation observation = new Observation();
        observation.setCode(new CodeableConcept().setText("Elevated blood glucose"));
        observation.setValue(new StringType("250 mg/dL"));

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(observation);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertNotNull(medicalCase.symptoms());
        assertTrue(medicalCase.symptoms().contains("Elevated blood glucose"));
    }

    @Test
    @DisplayName("convertBundleToMedicalCase determines MEDIUM urgency for ambulatory encounter")
    void convertBundle_MediumUrgency() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(25);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("J45.9"));
        condition.setCode(code);

        Encounter encounter = new Encounter();
        encounter.setStatus(Enumerations.EncounterStatus.COMPLETED);
        CodeableConcept classConcept = new CodeableConcept();
        classConcept.addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));
        encounter.addClass_(classConcept);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);
        bundle.addEntry().setResource(encounter);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals(UrgencyLevel.MEDIUM, medicalCase.urgencyLevel());
        assertEquals(CaseType.SECOND_OPINION, medicalCase.caseType());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase uses condition severity for urgency")
    void convertBundle_SeverityCritical() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(50);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("C34.9"));
        condition.setCode(code);
        CodeableConcept severity = new CodeableConcept();
        severity.setText("critical");
        condition.setSeverity(severity);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals(UrgencyLevel.CRITICAL, medicalCase.urgencyLevel());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase determines specialty from ICD-10 fallback")
    void convertBundle_FallbackSpecialtyCardiology() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I21.9")
                .setDisplay("Acute myocardial infarction"));
        condition.setCode(code);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        when(medicalSpecialtyRepository.findAll()).thenReturn(List.of());

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals("Cardiology", medicalCase.requiredSpecialty());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase determines specialty from condition text fallback")
    void convertBundle_SpecialtyFromConditionText() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("E11.9")
                .setDisplay("Type 2 diabetes"));
        condition.setCode(code);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        when(medicalSpecialtyRepository.findAll()).thenReturn(List.of());

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals("Endocrinology", medicalCase.requiredSpecialty());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase matches ICD-10 prefix to specialty range")
    void convertBundle_SpecialtyByIcd10Range() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("I26.9"));
        condition.setCode(code);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        MedicalSpecialty cardiology = new MedicalSpecialty(
                "spec-001", "Cardiology", "cardiology",
                "Heart and vascular",
                List.of("I00-I99"), List.of());
        when(medicalSpecialtyRepository.findAll()).thenReturn(List.of(cardiology));

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals("Cardiology", medicalCase.requiredSpecialty());
    }

    @Test
    @DisplayName("convertBundleToMedicalCase handles condition with severity=high")
    void convertBundle_SeverityHigh() {
        Bundle bundle = new Bundle();

        Patient patient = new Patient();
        LocalDate birthDate = LocalDate.now().minusYears(40);
        patient.setBirthDate(Date.from(birthDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));

        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
                .setSystem("http://hl7.org/fhir/sid/icd-10")
                .setCode("J45.9"));
        condition.setCode(code);
        CodeableConcept severity = new CodeableConcept();
        severity.setText("high");
        condition.setSeverity(severity);

        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(condition);

        MedicalCase medicalCase = adapter.convertBundleToMedicalCase(bundle);
        assertEquals(UrgencyLevel.HIGH, medicalCase.urgencyLevel());
    }
}
