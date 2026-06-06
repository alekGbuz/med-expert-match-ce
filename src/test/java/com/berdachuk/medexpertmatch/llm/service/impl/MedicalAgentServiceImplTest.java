package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MedicalAgentServiceImpl.
 * Tests the stripLlmReasoning method to ensure LLM internal reasoning is properly removed.
 */
class MedicalAgentServiceImplTest {
    private String stripLlmReasoningLogic(String response) {
        return LlmResponseSanitizer.stripLlmReasoning(response);
    }

    @Test
    @DisplayName("Should return null when input is null")
    void testStripLlmReasoning_NullInput() {
        String result = stripLlmReasoningLogic(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should return empty string when input is blank")
    void testStripLlmReasoning_BlankInput() {
        String result = stripLlmReasoningLogic("");
        assertEquals("", result);

        result = stripLlmReasoningLogic("   ");
        assertEquals("", result.trim());
    }

    @Test
    @DisplayName("Should return unchanged text when no reasoning markers")
    void testStripLlmReasoning_NoReasoningMarkers() {
        String input = "This is a normal response without any reasoning markers.";
        String result = stripLlmReasoningLogic(input);
        assertEquals(input, result);
    }

    @ParameterizedTest
    @MethodSource("reasoningHeaderProvider")
    @DisplayName("Should strip reasoning headers from response")
    void testStripLlmReasoning_ReasoningHeaders(String header, String input, String expectedContains) {
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
        assertTrue(result.contains(expectedContains) || result.startsWith(expectedContains.split("\n")[0]),
                "Expected result to contain: " + expectedContains + ", but got: " + result);
        assertFalse(result.toLowerCase().startsWith(header.toLowerCase()),
                "Result should not start with reasoning header: " + header);
    }

    private static Stream<Arguments> reasoningHeaderProvider() {
        return Stream.of(
                // "Understand the Goal:" header
                Arguments.of(
                        "Understand the Goal:",
                        "Understand the Goal: The user wants to find a doctor.\n\nBased on the case analysis, I recommend...",
                        "Based on the case analysis"
                ),
                // "Analyze the" header
                Arguments.of(
                        "Analyze the",
                        "Analyze the case details.\n\nThe patient presents with chest pain...",
                        "The patient presents"
                ),
                // "Step 1:" header
                Arguments.of(
                        "Step 1:",
                        "Step 1: Review the symptoms.\n\nThe recommended specialist is...",
                        "The recommended specialist"
                ),
                // "Thought:" header
                Arguments.of(
                        "Thought:",
                        "Thought: I need to analyze this case.\n\nThe analysis shows...",
                        "The analysis shows"
                ),
                // "Thinking:" header
                Arguments.of(
                        "Thinking:",
                        "Thinking: Let me consider the options.\n\nHere is my recommendation...",
                        "Here is my recommendation"
                ),
                // "Reasoning:" header
                Arguments.of(
                        "Reasoning:",
                        "Reasoning: Based on the symptoms...\n\nThe diagnosis suggests...",
                        "The diagnosis suggests"
                ),
                // "Analysis:" header
                Arguments.of(
                        "Analysis:",
                        "Analysis: The case involves...\n\nMy conclusion is...",
                        "My conclusion is"
                ),
                // "Let me think" header
                Arguments.of(
                        "Let me think",
                        "Let me think about this.\n\nThe best approach would be...",
                        "The best approach would be"
                ),
                // "Let's analyze" header
                Arguments.of(
                        "Let's analyze",
                        "Let's analyze the case.\n\nThe findings indicate...",
                        "The findings indicate"
                ),
                // "First, I'll" header
                Arguments.of(
                        "First, I'll",
                        "First, I'll review the patient history.\n\nThe patient should...",
                        "The patient should"
                ),
                // "I need to" header
                Arguments.of(
                        "I need to",
                        "I need to check the symptoms.\n\nBased on my analysis...",
                        "Based on my analysis"
                ),
                // "The task is" header
                Arguments.of(
                        "The task is",
                        "The task is to find a specialist.\n\nI recommend consulting...",
                        "I recommend consulting"
                ),
                // "Key Information" header
                Arguments.of(
                        "Key Information",
                        "Key Information: Patient age 45.\n\nThe specialist needed is...",
                        "The specialist needed is"
                )
        );
    }

    @Test
    @DisplayName("Should handle 'thought' prefix (case-insensitive)")
    void testStripLlmReasoning_ThoughtPrefix() {
        String input = "Thought: I should analyze this case.\n\nThe recommended doctor is Dr. Smith.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("The recommended doctor") || result.contains("Dr. Smith"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should handle lowercase 'thought' prefix")
    void testStripLlmReasoning_LowercaseThought() {
        String input = "thought: analyzing the case...\n\nThe patient needs a cardiologist.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("cardiologist") || result.startsWith("The patient"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should remove unused markers like <unused94>")
    void testStripLlmReasoning_UnusedMarkers() {
        String input = "<unused94><unused95>Here is the actual response.";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("<unused"));
        assertTrue(result.contains("actual response"));
    }

    @Test
    @DisplayName("Should extract content from JSON code blocks")
    void testStripLlmReasoning_JsonCodeBlock() {
        String input = "```json\n{\"requiredSpecialty\": \"Cardiology\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("```json"));
        assertTrue(result.contains("Cardiology"));
    }

    @Test
    @DisplayName("Should extract content from generic code blocks")
    void testStripLlmReasoning_GenericCodeBlock() {
        String input = "```\n{\"urgencyLevel\": \"HIGH\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("HIGH"));
    }

    @Test
    @DisplayName("Should handle multiple reasoning sections")
    void testStripLlmReasoning_MultipleReasoningSections() {
        String input = "Understand the Goal: Find a doctor.\n\nAnalyze the case: Patient has chest pain.\n\nThe recommended specialist is a cardiologist.";
        String result = stripLlmReasoningLogic(input);
        // Should strip at least the first reasoning section
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("Should handle response starting with 'Based on'")
    void testStripLlmReasoning_BasedOnPattern() {
        String input = "Thought: Let me analyze.\n\nBased on the routing results, the best facility is...";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Based on") || result.contains("best facility"));
    }

    @Test
    @DisplayName("Should preserve normal medical response")
    void testStripLlmReasoning_PreserveMedicalResponse() {
        String input = """
                Based on the case analysis, the patient requires immediate cardiac evaluation.
                
                **Recommended Specialty:** Cardiology
                **Urgency Level:** HIGH
                
                The patient should be referred to a cardiologist for further evaluation.
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Cardiology"));
        assertTrue(result.contains("HIGH"));
        assertTrue(result.contains("cardiologist"));
    }

    @Test
    @DisplayName("Should handle response with only reasoning (no actual content)")
    void testStripLlmReasoning_OnlyReasoning() {
        String input = "Thought: I need to analyze this case.";
        String result = stripLlmReasoningLogic(input);
        // Should return something, even if it's the stripped version
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle empty response after stripping")
    void testStripLlmReasoning_EmptyAfterStripping() {
        String input = "Thought:\n\n";
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle complex nested reasoning")
    void testStripLlmReasoning_ComplexNestedReasoning() {
        String input = """
                Understand the Goal: The user wants a doctor recommendation.
                
                Step 1: Analyze the symptoms.
                The patient has chest pain.
                
                Step 2: Determine specialty.
                Based on symptoms, cardiology is needed.
                
                The recommended specialist is a cardiologist.
                """;
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
        // Should have stripped at least the first reasoning section
        assertFalse(result.toLowerCase().startsWith("understand the goal"));
    }

    @Test
    @DisplayName("Should handle reasoning with JSON at the end")
    void testStripLlmReasoning_ReasoningWithJson() {
        String input = "Thought: Analyzing the case.\n\n```json\n{\"specialty\": \"Neurology\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Neurology"));
        assertFalse(result.contains("Thought:"));
        assertFalse(result.contains("```"));
    }

    @Test
    @DisplayName("Should handle 'Step 2:' reasoning header")
    void testStripLlmReasoning_Step2Header() {
        String input = "Step 2: Now I will provide the recommendation.\n\nThe patient should see a neurologist.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("neurologist") || result.contains("patient"));
        assertFalse(result.startsWith("Step 2:"));
    }

    @Test
    @DisplayName("Should strip MedGemma constraint checklist before Case Summary")
    void testStripLlmReasoning_ConstraintChecklistBeforeCaseSummary() {
        String input = """
                Recommendations: YES (clear next steps)
                5. Clear/Well-structured: YES
                6. Medical Terminology: YES
                Confidence Score: 5/5 - I am confident I can meet all constraints.
                Mental Sandbox Simulation:
                Scenario 1: If the tool results were empty, I would provide a summary.
                Strategizing complete. I will now proceed with generating the response.Case Summary:
                The patient is a 30-year-old individual presenting with peripheral vascular disease.
                Matched Doctors:
                Dr. Example
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("Case Summary:"));
        assertFalse(result.contains("Mental Sandbox"));
        assertFalse(result.contains("Confidence Score"));
        assertFalse(result.contains("Recommendations: YES"));
    }

    @Test
    @DisplayName("Should strip MedGemma Mental Sandbox CoT before Case Summary")
    void testStripLlmReasoning_MentalSandboxBeforeCaseSummary() {
        String input = """
                thought
                The user wants me to generate a comprehensive response.
                Mental Sandbox Simulation:
                Scenario 1: Tool Results Present.
                Strategizing complete. I will now proceed with generating the response following these steps.Case Summary:
                The patient is a 30-year-old individual presenting with peripheral vascular disease.
                Matched Doctors:
                Dr. Example
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("Case Summary:"));
        assertFalse(result.contains("Mental Sandbox"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should strip thought planning before Case Summary without Mental Sandbox")
    void testStripLlmReasoning_ThoughtPlanningBeforeCaseSummary() {
        String input = """
                thought
                The user wants me to generate a response based on the provided case analysis.
                Summarize the Case: The case involves peripheral vascular disease.
                Plan:
                Write a case summary using only the provided information.
                Case Summary:
                A 30-year-old patient presents with peripheral vascular disease unspecified.
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("Case Summary:"));
        assertFalse(result.toLowerCase().contains("summarize the case"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should strip thought planning before Case Summary section without colon")
    void testStripLlmReasoning_ThoughtBeforeCaseSummaryNoColon() {
        String input = """
                thought
                Constraint Checklist & Confidence Score:
                Confidence Score: 5/5
                Mental Sandbox Simulation:
                Scenario 1 (Age provided): summary uses age 30.
                Strategizing complete. Proceeding with response generation. Case Summary
                A 30-year-old patient presents with peripheral vascular disease unspecified.
                Clinical Presentation
                The patient complains of stiffness and wrist pain.
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("Case Summary"));
        assertFalse(result.contains("Mental Sandbox"));
        assertFalse(result.contains("Confidence Score"));
    }

    @Test
    @DisplayName("Should wrap reasoning in collapsible details for chat display")
    void testFormatForChatDisplay_WrapsReasoning() {
        String input = """
                thought
                The user wants a clinical case description with several constraints.
                Mental Sandbox Simulation:
                Scenario 1: use exact age.
                Strategizing complete. Case Summary
                A 30-year-old patient presents with peripheral vascular disease.
                """;
        String result = LlmResponseSanitizer.formatForChatDisplay(input);
        assertTrue(result.contains("class=\"llm-thinking\""));
        assertTrue(result.contains("<summary>Model reasoning</summary>"));
        assertTrue(result.contains("A 30-year-old patient presents with peripheral vascular disease."));
        int detailsEnd = result.indexOf("</details>");
        assertTrue(detailsEnd > 0);
        assertTrue(result.substring(detailsEnd).contains("A 30-year-old patient"));
        assertFalse(result.substring(detailsEnd).contains("Mental Sandbox"));
    }

    @Test
    @DisplayName("Should split numbered Case Summary after strategizing block")
    void testStripLlmReasoning_NumberedCaseSummaryAfterStrategizing() {
        String input = """
                The user wants me to generate a response based on the provided case analysis.
                Summarize the Case:
                Patient is 30 years old.
                Mental Sandbox Simulation:
                Scenario 1: Vascular Surgeon match.
                Strategizing complete. I will now generate the response following these steps.1. Case Summary
                This 30-year-old patient presents with peripheral vascular disease unspecified.
                2. Matching Rationale Explanation
                The primary complaint points towards vascular evaluation.
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("1. Case Summary") || result.startsWith("This 30-year-old"));
        assertFalse(result.contains("Mental Sandbox"));
        assertFalse(result.contains("The user wants me to generate"));
    }

    @Test
    @DisplayName("Should ignore checklist Case Summary and use final clinical section")
    void testStripLlmReasoning_SkipsChecklistCaseSummary() {
        String input = """
                The user wants me to generate a clinical case description.
                Case Summary: Brief overview? YES
                Clinical Presentation: Symptoms? YES
                Constraint Checklist & Confidence Score:
                Confidence Score: 5/5
                Mental Sandbox Simulation:
                Scenario 1: use exact age.
                Strategizing complete: I will now proceed. Case Summary:
                This 30-year-old patient presented with peripheral vascular disease unspecified.
                Clinical Presentation:
                The patient reports stiffness and wrist pain.
                """;
        String result = LlmResponseSanitizer.formatForChatDisplay(input);
        int detailsEnd = result.indexOf("</details>");
        String visible = detailsEnd > 0 ? result.substring(detailsEnd) : result;
        assertTrue(visible.contains("This 30-year-old patient presented"));
        assertFalse(visible.contains("Brief overview? YES"));
        assertFalse(visible.contains("Mental Sandbox"));
    }

    @Test
    @DisplayName("Should split doctor-match planning ending with glued Case Summary colon")
    void testFormatForChatDisplay_DoctorMatchGluedCaseSummary() {
        String input = """
                The user wants me to generate a response based on the provided case analysis and tool execution results.
                Summarize the Case: Based only on the case analysis, summarize the key points:
                Chief Complaint: Peripheral vascular disease unspecified.
                Mental Sandbox:
                Strategizing complete. I will now proceed with generating the response based on these points.Case Summary:
                The patient is a 30-year-old individual presenting to an Emergency Department.
                Matching Rationale Explanation:
                Dr. Stasia Wunsch was matched based on specialty alignment.
                """;
        String result = LlmResponseSanitizer.formatForChatDisplay(input);
        assertTrue(result.contains("llm-thinking"));
        assertTrue(result.contains("The patient is a 30-year-old individual"));
        int detailsEnd = result.indexOf("</details>");
        assertTrue(detailsEnd > 0);
        assertFalse(result.substring(detailsEnd).contains("The user wants me to generate"));
        assertFalse(result.substring(detailsEnd).contains("Mental Sandbox"));
    }

    @Test
    @DisplayName("Should handle 'Step 3:' reasoning header")
    void testStripLlmReasoning_Step3Header() {
        String input = "Step 3: Final recommendation.\n\nConsult with an oncologist for further evaluation.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("oncologist") || result.contains("Consult"));
        assertFalse(result.startsWith("Step 3:"));
    }
}
