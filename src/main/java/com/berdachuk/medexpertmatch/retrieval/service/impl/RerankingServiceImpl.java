package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.service.RerankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RerankingServiceImpl implements RerankingService {

    private final ChatClient rerankingChatClient;
    private final PromptTemplate rerankingDoctorsPromptTemplate;
    private final MedicalCaseRepository medicalCaseRepository;
    private final boolean enabled;

    public RerankingServiceImpl(
            @Nullable @Qualifier("rerankingChatClient") ChatClient rerankingChatClient,
            @Qualifier("rerankingDoctorsPromptTemplate") PromptTemplate rerankingDoctorsPromptTemplate,
            MedicalCaseRepository medicalCaseRepository,
            @Value("${medexpertmatch.retrieval.reranking.enabled:false}") boolean enabled) {
        this.rerankingChatClient = rerankingChatClient;
        this.rerankingDoctorsPromptTemplate = rerankingDoctorsPromptTemplate;
        this.medicalCaseRepository = medicalCaseRepository;
        this.enabled = enabled;
    }

    @Override
    public List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK) {
        if (!enabled || candidates == null || candidates.isEmpty() || rerankingChatClient == null) {
            return candidates;
        }
        if (candidates.size() <= topK) {
            return candidates;
        }

        try {
            Optional<MedicalCase> medicalCaseOpt = medicalCaseRepository.findById(caseId);
            if (medicalCaseOpt.isEmpty()) {
                log.warn("Case {} not found, skipping re-ranking", caseId);
                return candidates;
            }
            MedicalCase medicalCase = medicalCaseOpt.get();

            List<DoctorMatch> topCandidates = candidates.stream()
                    .sorted(Comparator.comparing(DoctorMatch::matchScore).reversed())
                    .limit(Math.min(candidates.size(), 50))
                    .toList();

            StringBuilder candidateList = new StringBuilder();
            for (int i = 0; i < topCandidates.size(); i++) {
                DoctorMatch match = topCandidates.get(i);
                Doctor doctor = match.doctor();
                candidateList.append(String.format("%d. Dr. %s (ID: %s, Score: %.1f, Specialty: %s)%n",
                        i + 1, doctor.name(), doctor.id(), match.matchScore(),
                        doctor.specialties() != null ? String.join(", ", doctor.specialties()) : "N/A"));
            }

            String promptText = rerankingDoctorsPromptTemplate.render(Map.of(
                    "chiefComplaint", nullToEmpty(medicalCase.chiefComplaint()),
                    "symptoms", nullToEmpty(medicalCase.symptoms()),
                    "diagnosis", nullToEmpty(medicalCase.currentDiagnosis()),
                    "icd10Codes", medicalCase.icd10Codes() != null ? String.join(", ", medicalCase.icd10Codes()) : "N/A",
                    "requiredSpecialty", nullToEmpty(medicalCase.requiredSpecialty()),
                    "urgency", medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : "N/A",
                    "candidates", candidateList.toString()
            ));

            String response = rerankingChatClient.prompt().user(promptText).call().content();
            if (response == null || response.isBlank()) {
                log.warn("Empty reranking response, returning original candidates");
                return candidates;
            }

            List<Integer> ranking = parseLineBasedIndices(response);
            if (ranking == null || ranking.isEmpty()) {
                return candidates;
            }

            List<DoctorMatch> reranked = new ArrayList<>();
            for (int idx : ranking) {
                if (idx >= 0 && idx < topCandidates.size()) {
                    reranked.add(topCandidates.get(idx));
                }
            }

            for (DoctorMatch match : topCandidates) {
                if (!reranked.contains(match)) {
                    reranked.add(match);
                }
            }

            log.debug("Re-ranked {} candidates for case {}", reranked.size(), caseId);
            return reranked;
        } catch (Exception e) {
            log.warn("Re-ranking failed for case {}: {}", caseId, e.getMessage());
            return candidates;
        }
    }

    private static List<Integer> parseLineBasedIndices(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
