package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseContextBundleServiceTest {

    private final MedicalCaseRepository medicalCaseRepository = mock(MedicalCaseRepository.class);
    private final CaseContextBundleService service = new CaseContextBundleServiceImpl(medicalCaseRepository);

    @Test
    @DisplayName("build includes core sections for existing case")
    void buildsBundleForCase() {
        String caseId = "6a1c68963a08e800010de68e";
        MedicalCase medicalCase = new MedicalCase(
                caseId, 65, "chest pain", "pain", "ACS", List.of(), List.of(),
                UrgencyLevel.HIGH, "Cardiology", CaseType.INPATIENT, null, null);
        when(medicalCaseRepository.findById(caseId)).thenReturn(Optional.of(medicalCase));

        CaseContextBundle bundle = service.build(caseId, CaseContextIntent.MATCH);

        assertEquals(caseId, bundle.caseId());
        assertTrue(bundle.coreSections().size() >= 2);
        assertTrue(bundle.summary().contains(caseId));
    }
}
