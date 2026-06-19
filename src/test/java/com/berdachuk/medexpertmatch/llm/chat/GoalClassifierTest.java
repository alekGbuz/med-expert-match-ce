package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class GoalClassifierTest {

    @ParameterizedTest
    @CsvSource({
            "MATCH_DOCTORS, MATCH",
            "ANALYZE_CASE, ANALYZE",
            "ROUTE_CASE, ROUTE",
            "TRIAGE_INTAKE, MATCH",
            "SEARCH_EVIDENCE, EVIDENCE",
            "GENERATE_RECOMMENDATIONS, CHAT_AUTO",
            "GENERAL_QUESTION, CHAT_AUTO"
    })
    @DisplayName("toContextIntent maps each GoalType to the correct CaseContextIntent")
    void toContextIntentMapsCorrectly(String goalTypeName, String expectedIntentName) {
        GoalType goalType = GoalType.valueOf(goalTypeName);
        CaseContextIntent expected = CaseContextIntent.valueOf(expectedIntentName);
        assertEquals(expected, GoalClassifier.toContextIntent(goalType));
    }

@Test
    @DisplayName("classify returns general for null or blank input")
    void classifyReturnsGeneralForNull() {
        org.springframework.ai.chat.client.ChatClient chatClient =
                org.mockito.Mockito.mock(org.springframework.ai.chat.client.ChatClient.class);
        org.springframework.ai.chat.prompt.PromptTemplate template =
                org.mockito.Mockito.mock(org.springframework.ai.chat.prompt.PromptTemplate.class);
        GoalClassifier classifier = new GoalClassifier(
                chatClient, template, template,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new com.berdachuk.medexpertmatch.core.util.LlmCallLimiter(1, 1, 1, 1),
                mock(ApplicationEventPublisher.class));

        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify(null).goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("").goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("   ").goalType());
    }

    @Test
    @DisplayName("parseClassification handles ultra-compact JSON with short keys")
    void parseClassificationShortKeys() {
        ObjectMapper mapper = new ObjectMapper();
        GoalClassifier classifier = new GoalClassifier(
                mock(org.springframework.ai.chat.client.ChatClient.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mapper,
                new com.berdachuk.medexpertmatch.core.util.LlmCallLimiter(1, 1, 1, 1),
                mock(ApplicationEventPublisher.class));

        String shortJson = "{\"g\":\"MATCH_DOCTORS\",\"s\":\"find cardiologist\",\"u\":false}";
        var result = classifier.parseClassification(shortJson, Optional.empty());
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertEquals("find cardiologist", result.summary());
    }

    @Test
    @DisplayName("parseClassification handles legacy long keys for backward compatibility")
    void parseClassificationLegacyKeys() {
        ObjectMapper mapper = new ObjectMapper();
        GoalClassifier classifier = new GoalClassifier(
                mock(org.springframework.ai.chat.client.ChatClient.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mapper,
                new com.berdachuk.medexpertmatch.core.util.LlmCallLimiter(1, 1, 1, 1),
                mock(ApplicationEventPublisher.class));

        String legacyJson = "{\"goalType\":\"ANALYZE_CASE\",\"summary\":\"analyze case\",\"useSessionCase\":true}";
        var result = classifier.parseClassification(legacyJson, Optional.empty());
        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertEquals("analyze case", result.summary());
    }
}
