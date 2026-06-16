package com.berdachuk.medexpertmatch.caseanalysis.service.impl;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseAnalysisServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private PromptTemplate caseAnalysisSystemPromptTemplate;
    @Mock
    private PromptTemplate caseAnalysisUserPromptTemplate;
    @Mock
    private PromptTemplate icd10ExtractionSystemPromptTemplate;
    @Mock
    private PromptTemplate icd10ExtractionUserPromptTemplate;
    @Mock
    private PromptTemplate urgencyClassificationSystemPromptTemplate;
    @Mock
    private PromptTemplate urgencyClassificationUserPromptTemplate;
    @Mock
    private PromptTemplate specialtyDeterminationSystemPromptTemplate;
    @Mock
    private PromptTemplate specialtyDeterminationUserPromptTemplate;
    @Mock
    private LlmCallLimiter llmCallLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CaseAnalysisServiceImpl createService() {
        return new CaseAnalysisServiceImpl(
                chatClient, caseAnalysisSystemPromptTemplate, caseAnalysisUserPromptTemplate,
                icd10ExtractionSystemPromptTemplate, icd10ExtractionUserPromptTemplate,
                urgencyClassificationSystemPromptTemplate, urgencyClassificationUserPromptTemplate,
                specialtyDeterminationSystemPromptTemplate, specialtyDeterminationUserPromptTemplate,
                "test-model", objectMapper, llmCallLimiter);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when medicalCase is null")
    void analyzeCaseNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.analyzeCase(null));
    }

    @Test
    @DisplayName("returns empty result when LLM response is empty")
    void analyzeCaseEmptyResponseReturnsEmpty() {
        when(caseAnalysisSystemPromptTemplate.render(any())).thenReturn("system");
        when(caseAnalysisUserPromptTemplate.render(any())).thenReturn("user");
        when(llmCallLimiter.execute(eq(LlmClientType.CLINICAL), any(Supplier.class))).thenReturn("");

        var service = createService();
        MedicalCase mc = new MedicalCase("case-1", 30, "Chest pain", null, null,
                List.of(), List.of(), null, "Cardiology", null, null, null, null, null);

        CaseAnalysisResult result = service.analyzeCase(mc);
        assertTrue(result.clinicalFindings().isEmpty());
    }

    @Test
    @DisplayName("extractICD10Codes throws when medicalCase is null")
    void extractICD10CodesNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.extractICD10Codes(null));
    }

    @Test
    @DisplayName("classifyUrgency throws when medicalCase is null")
    void classifyUrgencyNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.classifyUrgency(null));
    }

    @Test
    @DisplayName("determineRequiredSpecialty throws when medicalCase is null")
    void determineRequiredSpecialtyNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.determineRequiredSpecialty(null));
    }
}
