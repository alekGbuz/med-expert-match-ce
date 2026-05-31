package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CaseContextBundleServiceImpl implements CaseContextBundleService {

    private final MedicalCaseRepository medicalCaseRepository;

    public CaseContextBundleServiceImpl(MedicalCaseRepository medicalCaseRepository) {
        this.medicalCaseRepository = medicalCaseRepository;
    }

    @Override
    public CaseContextBundle build(String caseId, CaseContextIntent intent) {
        Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            return new CaseContextBundle(
                    caseId,
                    intent,
                    List.of(),
                    List.of(),
                    "Case not found: " + caseId,
                    Map.of("found", "false"));
        }
        MedicalCase medicalCase = caseOpt.get();
        List<String> core = new ArrayList<>();
        core.add("caseId=" + caseId);
        if (medicalCase.urgencyLevel() != null) {
            core.add("urgency=" + medicalCase.urgencyLevel());
        }
        if (medicalCase.requiredSpecialty() != null && !medicalCase.requiredSpecialty().isBlank()) {
            core.add("requiredSpecialty=" + medicalCase.requiredSpecialty());
        }
        if (medicalCase.caseType() != null) {
            core.add("caseType=" + medicalCase.caseType());
        }

        List<String> maybe = new ArrayList<>();
        if (medicalCase.patientAge() != null) {
            maybe.add("patientAge=" + medicalCase.patientAge());
        }
        if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
            maybe.add("icd10Count=" + medicalCase.icd10Codes().size());
        }

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("found", "true");
        attributes.put("intent", intent.name());
        attributes.put("coreSectionCount", String.valueOf(core.size()));

        String summary = "Case " + caseId + " for " + intent.name()
                + ": urgency=" + medicalCase.urgencyLevel()
                + ", specialty=" + nullToDash(medicalCase.requiredSpecialty());

        return new CaseContextBundle(caseId, intent, List.copyOf(core), List.copyOf(maybe), summary, Map.copyOf(attributes));
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
