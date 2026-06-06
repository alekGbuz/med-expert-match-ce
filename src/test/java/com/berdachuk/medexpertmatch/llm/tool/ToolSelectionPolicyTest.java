package com.berdachuk.medexpertmatch.llm.tool;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectionPolicyTest {

    private static final String CASE_ID = "6a23f05200155d711484cf69";

    @Test
    @DisplayName("analyze_case when case ID available and goal is ANALYZE_CASE")
    void analyzeCaseWithId() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.ANALYZE_CASE, true, CASE_ID, "detail the clinical case")
                .orElseThrow();
        assertEquals("analyze_case", choice.toolName());
        assertEquals(CASE_ID, choice.args().get("caseId"));
    }

    @Test
    @DisplayName("match_doctors_to_case when case ID available and goal is MATCH_DOCTORS")
    void matchDoctorsWithId() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.MATCH_DOCTORS, true, CASE_ID, "find more specialists")
                .orElseThrow();
        assertEquals("match_doctors_to_case", choice.toolName());
        assertEquals(CASE_ID, choice.args().get("caseId"));
    }

    @Test
    @DisplayName("analyze_case_text when no case ID in hints")
    void analyzeCaseTextWithoutId() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.ANALYZE_CASE, false, null, "Patient is 45 with chest pain...")
                .orElseThrow();
        assertEquals("analyze_case_text", choice.toolName());
        assertTrue(choice.args().isEmpty());
    }

    @Test
    @DisplayName("match_doctors_from_text when no case ID in hints")
    void matchFromTextWithoutId() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.MATCH_DOCTORS, false, null, "55yo with progressive dyspnea")
                .orElseThrow();
        assertEquals("match_doctors_from_text", choice.toolName());
    }

    @Test
    @DisplayName("match_facilities_for_case when routing with case ID")
    void routeWithCaseId() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.ROUTE_CASE, true, CASE_ID, "route patient to facility")
                .orElseThrow();
        assertEquals("match_facilities_for_case", choice.toolName());
        assertEquals(CASE_ID, choice.args().get("caseId"));
    }

    @Test
    @DisplayName("query_pubmed for literature evidence intent")
    void pubmedEvidence() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.SEARCH_EVIDENCE, false, null, "search pubmed for diabetes treatment")
                .orElseThrow();
        assertEquals("query_pubmed", choice.toolName());
    }

    @Test
    @DisplayName("search_clinical_guidelines for guideline evidence intent")
    void guidelineEvidence() {
        ToolSelectionPolicy.ToolChoice choice = ToolSelectionPolicy.resolve(
                        GoalType.SEARCH_EVIDENCE, false, null, "clinical practice guidelines for hypertension")
                .orElseThrow();
        assertEquals("search_clinical_guidelines", choice.toolName());
    }

    @Test
    @DisplayName("no tool for general informational questions")
    void noToolForGeneralQuestion() {
        assertFalse(ToolSelectionPolicy.resolve(
                GoalType.GENERAL_QUESTION, false, null, "What is GraphRAG?").isPresent());
    }

    @ParameterizedTest
    @CsvSource({
            "analyze_case_text,ANALYZE_CASE,analyze_case",
            "match_doctors_from_text,MATCH_DOCTORS,match_doctors_to_case"
    })
    @DisplayName("correctToolName remaps text tools to case-ID tools when session has case ID")
    void correctToolNameRemaps(String requested, GoalType goal, String expected) {
        assertEquals(expected, ToolSelectionPolicy.correctToolName(requested, goal, CASE_ID));
    }

    @Test
    @DisplayName("correctToolName leaves tool unchanged when no session case ID")
    void correctToolNameNoSessionCase() {
        assertEquals("analyze_case_text",
                ToolSelectionPolicy.correctToolName("analyze_case_text", GoalType.ANALYZE_CASE, null));
    }

    @Test
    @DisplayName("matchesExpected compares tool name and required args")
    void matchesExpectedHelper() {
        ToolSelectionPolicy.ToolChoice expected = new ToolSelectionPolicy.ToolChoice(
                "analyze_case", Map.of("caseId", CASE_ID));
        ToolSelectionPolicy.ToolChoice actual = new ToolSelectionPolicy.ToolChoice(
                "analyze_case", Map.of("caseId", CASE_ID));
        assertTrue(ToolSelectionPolicy.matchesExpected(expected, actual));
    }
}
