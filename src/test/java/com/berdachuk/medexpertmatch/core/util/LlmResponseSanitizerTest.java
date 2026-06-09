package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LlmResponseSanitizer#toHumanReadable(String)} (M74).
 *
 * The LLM (medgemma1.5:4b) sometimes returns responses that contain
 * a JSON block (e.g. {@code { "requiredSpecialty": ..., "icd10Codes": [...] }})
 * inside a {@code Response} wrapper, or as the entire response. The
 * pre-M74 sanitizer caught only the pure-JSON case and replaced it
 * with a generic message; the embedded-JSON case slipped through and
 * the chat UI displayed raw JSON to the user.
 */
class LlmResponseSanitizerTest {

    // ------------------------------------------------------------------
    // 1. JSON embedded in a "Response" wrapper is rendered as prose
    //    with friendly labels and the JSON block stripped
    // ------------------------------------------------------------------

    @Test
    void rendersEmbeddedJsonAfterMatchingRationaleAsProse() {
        String llmOutput = """
                **Matching Rationale:**
                The match is borderline because the doctor pool is thin in Oncology.

                Response
                {
                  "requiredSpecialty": "Urologic Oncology / Renal Cancer",
                  "urgencyLevel": "HIGH",
                  "clinicalFindings": [
                    "Malignant neoplasm of kidney except renal pelvis unspecified"
                  ],
                  "icd10Codes": [
                    "C64.20"
                  ],
                  "caseSummary": "A 64-year-old patient has a diagnosis of malignant neoplasm of kidney except renal pelvis unspecified, requiring oncology consultation for cancer management."
                }
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        // The narrative before the JSON is preserved
        assertTrue(result.contains("**Matching Rationale:**"),
                "narrative prefix should be preserved");
        assertTrue(result.contains("borderline because the doctor pool is thin"),
                "the prose before the JSON must survive");

        // The JSON object is gone — no raw '{' / '"requiredSpecialty"' markers
        assertFalse(result.contains("\"requiredSpecialty\""),
                "raw JSON field name should be stripped");
        assertFalse(result.contains("\"icd10Codes\""),
                "raw JSON field name should be stripped");
        assertFalse(result.contains("\"caseSummary\""),
                "raw JSON field name should be stripped");

        // Friendly prose labels for the well-known fields
        assertTrue(result.contains("Recommended specialty: Urologic Oncology / Renal Cancer"),
                "requiredSpecialty -> Recommended specialty: <value>");
        assertTrue(result.contains("Urgency: HIGH"),
                "urgencyLevel -> Urgency: <value>");
        assertTrue(result.contains("Key findings: Malignant neoplasm of kidney except renal pelvis unspecified"),
                "clinicalFindings -> Key findings: <value>");
        assertTrue(result.contains("ICD-10 codes: C64.20"),
                "icd10Codes -> ICD-10 codes: <value>");
        assertTrue(result.contains("Summary: A 64-year-old patient has a diagnosis"),
                "caseSummary -> Summary: <value>");
    }

    // ------------------------------------------------------------------
    // 2. Pure-JSON responses are no longer replaced with the generic
    //    "[Data received; unable to display formatted response]"
    //    — the parsed fields are surfaced in prose
    // ------------------------------------------------------------------

    @Test
    void rendersPureJsonResponseAsInformativeProseFallback() {
        String llmOutput = """
                {
                  "requiredSpecialty": "Cardiology",
                  "urgencyLevel": "MEDIUM",
                  "caseSummary": "Chest pain workup."
                }
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        assertNotNull(result);
        assertFalse(result.contains("[Data received; unable to display formatted response]"),
                "the generic fallback message must be gone");
        assertFalse(result.contains("\"requiredSpecialty\""),
                "raw JSON should not appear in the output");
        assertTrue(result.contains("Recommended specialty: Cardiology"),
                "pure-JSON response must surface the parsed specialty");
        assertTrue(result.contains("Urgency: MEDIUM"),
                "pure-JSON response must surface the parsed urgency");
        assertTrue(result.contains("Summary: Chest pain workup."),
                "pure-JSON response must surface the parsed summary");
    }

    // ------------------------------------------------------------------
    // 3. A response with no JSON is untouched — narrative flows through
    //    as-is
    // ------------------------------------------------------------------

    @Test
    void leavesCleanNarrativeUntouched() {
        String input = """
                Case Summary
                A 64-year-old male with chest pain on exertion.

                Recommendations
                Refer to Cardiology for further workup.
                """;
        String expected = """
                Case Summary
                A 64-year-old male with chest pain on exertion.

                Recommendations
                Refer to Cardiology for further workup.""";
        assertEquals(expected, LlmResponseSanitizer.toHumanReadable(input));
    }

    // ------------------------------------------------------------------
    // 4. Malformed JSON is left untouched — never throw, never lose data
    // ------------------------------------------------------------------

    @Test
    void leavesMalformedJsonUntouched() {
        String malformed = """
                **Matching Rationale:** the tool gave back something off.

                Response
                {
                  "requiredSpecialty": "Cardiology"
                  "urgencyLevel": MEDIUM,,
                """;

        String result = LlmResponseSanitizer.toHumanReadable(malformed);

        // Narrative is preserved
        assertTrue(result.contains("**Matching Rationale:**"));
        assertTrue(result.contains("the tool gave back something off"));

        // Malformed JSON is not deleted (we never claim to render it)
        assertTrue(result.contains("requiredSpecialty"),
                "malformed JSON should be left in place; we do not guess");
    }

    // ------------------------------------------------------------------
    // 5. JSON mid-narrative (before AND after) is rendered as prose
    // ------------------------------------------------------------------

    @Test
    void rendersJsonMidNarrativeAndAppendsAfterText() {
        String llmOutput = """
                The case is on the borderline because the pool is thin.

                Response
                {
                  "requiredSpecialty": "Oncology",
                  "caseSummary": "Kidney cancer workup."
                }

                Clinicians should escalate to a urology consult.
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        assertTrue(result.contains("Recommended specialty: Oncology"));
        assertTrue(result.contains("Summary: Kidney cancer workup."));
        assertTrue(result.contains("Clinicians should escalate to a urology consult"),
                "prose AFTER the JSON block must survive");
        assertFalse(result.contains("\"requiredSpecialty\""),
                "raw JSON must be stripped");
    }

    // ------------------------------------------------------------------
    // 6. Null/empty input passes through
    // ------------------------------------------------------------------

    @Test
    void passesNullAndEmptyThrough() {
        assertEquals(null, LlmResponseSanitizer.toHumanReadable(null));
        assertEquals("", LlmResponseSanitizer.toHumanReadable(""));
        assertEquals("   ", LlmResponseSanitizer.toHumanReadable("   "));
    }

    // ------------------------------------------------------------------
    // 7. JSON-only with unknown fields is rendered as a bullet list
    // ------------------------------------------------------------------

    @Test
    void rendersUnknownJsonFieldsAsBulletList() {
        String llmOutput = """
                {
                  "recommendedDoctor": "Dr. Smith",
                  "estimatedWaitDays": 14
                }
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        assertTrue(result.contains("recommendedDoctor: Dr. Smith"));
        assertTrue(result.contains("estimatedWaitDays: 14"));
        assertFalse(result.contains("[Data received; unable to display formatted response]"));
    }

    // ------------------------------------------------------------------
    // 8. A pre-existing pure-JSON fallback (legacy contract) is preserved
    //    when the JSON has NO parseable fields at all (e.g. only
    //    numbers / null) — we still don't show a JSON blob
    // ------------------------------------------------------------------

    @Test
    void emptyJsonObjectFallsBackToGenericMessage() {
        String llmOutput = "{}";
        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        assertNotNull(result);
        // The legacy generic fallback is acceptable here because there is
        // literally no information to surface. We only assert it is NOT a
        // raw JSON blob.
        assertFalse(result.contains("{") || result.contains("}"),
                "empty JSON object should not be displayed as a JSON blob");
    }

    // ------------------------------------------------------------------
    // 9. Off-switch leaves the response untouched (operators debugging
    //    a prompt change can disable the renderer with
    //    medexpertmatch.llm.response.render-embedded-json: false)
    // ------------------------------------------------------------------

    @Test
    void leavesEmbeddedJsonUntouchedWhenRendererDisabled() {
        boolean previous = readToggle();
        try {
            LlmResponseSanitizer.setRenderEmbeddedJson(false);

            String llmOutput = """
                    **Matching Rationale:** borderline.

                    Response
                    {
                      "requiredSpecialty": "Urologic Oncology",
                      "urgencyLevel": "HIGH"
                    }
                    """;

            String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

            // Renderer off: the original JSON object is left in place
            assertTrue(result.contains("\"requiredSpecialty\""),
                    "renderer off should leave raw JSON in place");
            assertTrue(result.contains("\"urgencyLevel\""),
                    "renderer off should leave raw JSON in place");
        } finally {
            LlmResponseSanitizer.setRenderEmbeddedJson(previous);
        }
    }

    /**
     * Reflection-free way to read the current toggle value — we set it to
     * a known value and then restore the previous one in {@code finally}.
     */
    private static boolean readToggle() {
        // We don't expose a getter; flip the toggle twice with a sentinel
        // value would be observable. Instead use a known prior state
        // (tests run in isolation; default is true).
        return true;
    }
}
