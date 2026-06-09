# M74: Render LLM Response as Human-Readable, Not JSON

**Status:** **Done** (2026-06-09)  
**Created:** 2026-06-09  
**Depends on:** M71 (archived — LLM usage telemetry); existing `LlmResponseSanitizer.toHumanReadable()`

## Problem Statement

Verify run on case `6a27d7fcf6c1830001bdf9a5` (kidney cancer) and the
case-analysis flow both produce assistant responses that end with a raw
JSON block like:

```
**Matching Rationale:** …

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
  "caseSummary": "A 64-year-old patient has a diagnosis of malignant neoplasm …"
}
```

Operators see a "Model reasoning" expander and a "Response" section that
is a JSON code block instead of a paragraph they can read. The system
prompts already instruct the LLM to produce narrative sections
(`Case Summary`, `Clinical Presentation`, `Recommendations`, …) and
**forbid** JSON output, but `medgemma1.5:4b` produces it anyway.

`LlmResponseSanitizer.cleanJsonOnlyContent` (lines 413-443) only catches
**pure-JSON** responses (the entire response is JSON) and replaces them
with `[Data received; unable to display formatted response]`. It does
**not** catch:

1. JSON enclosed in a `Response` wrapper
2. JSON mixed with narrative text (narrative before, JSON after, or JSON
   with prose around it)
3. JSON with thinking/reasoning before the `Response` section
4. JSON-Lines output (`{...}\n{...}`)

The JS front-end (`chat.js` `splitReasoningFromText`) detects `Response`
and `llm-answer` divs but only renders the raw text as Markdown —
Markdown code fences are not auto-injected, so the JSON block just shows
as monospace text in the response panel.

## Goal

Render the chat panel's `Response` block as readable prose. **Only
affect the UI form renderer, not the server-side data path** — internal
consumers of the raw LLM response (caching, interpretation) must still
see the original JSON.

## Design Decision (corrected after first review)

The first implementation wired the JSON renderer into
`LlmResponseSanitizer.toHumanReadable()`. On review, this was too broad:
`toHumanReadable` is called by `MedicalAgentLlmSupportServiceImpl` for
cache hits and interpretation responses, which are server-side
processing, not UI display. The corrected design moves the renderer to
`LlmResponseSanitizer.formatForChatDisplay()` only — the function that
the chat panel and harness execution trace actually call.

| Path | Function | JSON rendering? |
|------|----------|-----------------|
| Cache + interpretation (server-side) | `toHumanReadable()` | **No** — kept original behavior, data path untouched |
| Chat panel + harness trace (UI) | `formatForChatDisplay()` | **Yes** — embedded JSON rendered as prose |
| Pure-JSON response in data path | `cleanJsonOnlyContent()` | **No** — kept generic fallback (internal consumers see the same message they always did) |
| Pure-JSON response in chat panel | `formatForChatDisplay()` → `renderEmbeddedJson()` | **Yes** — surfaces parsed fields instead of generic message |

## Changes

| Area | File | Change |
|------|------|--------|
| Sanitizer | `core/util/LlmResponseSanitizer.java` | New helper `renderEmbeddedJson(String)` that scans for any `{...}` block in the response, attempts Jackson parsing, and converts known case-analysis / match-result fields to prose. |
| UI integration | same | `formatForChatDisplay()` calls `renderEmbeddedJson()` on the content **after** `stripLlmReasoning()`. The server-side `toHumanReadable()` is **left untouched** so internal consumers of the LLM response still see the original JSON. |
| Field rendering | same | Render the well-known fields in a fixed order with friendly labels: `requiredSpecialty` → "Recommended specialty: …", `urgencyLevel` → "Urgency: …", `clinicalFindings` → "Key findings: …, …, …", `icd10Codes` → "ICD-10 codes: …, …, …", `caseSummary` → "Summary: …". Unknown fields fall through to a bullet list. |
| Block stripping | same | When the embedded JSON parses cleanly, strip it from the response and append the formatted prose after the existing narrative. Avoids double-rendering. |
| Failure mode | same | When parsing fails (malformed JSON), leave the response unchanged — never produce `[Data received; unable to display formatted response]` for a response that has useful narrative content. |
| Empty `{}` | same | `renderEmbeddedJson()` recognizes a single empty JSON object as the only thing in the response and returns the legacy generic message. |
| Config knob | `application.yml` (`medexpertmatch.llm.response.render-embedded-json: true`) | Allow operators to disable the formatter when debugging a prompt change. |
| Sanitizer unit tests | `LlmResponseSanitizerTest.java` (new) | 11 cases: 4 for `toHumanReadable` (data path must NOT render JSON, must keep generic fallback for pure-JSON, must pass through clean narrative, must pass null/empty); 7 for `formatForChatDisplay` (renders embedded JSON as prose, renders pure-JSON with informative fields, renders mid-narrative JSON, renders unknown fields, handles empty `{}`, off-switch leaves raw JSON, passes null/empty through). |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | `renderEmbeddedJson()` in `LlmResponseSanitizer` | Done |
| 2 | Field renderer with fixed-order labels and unknown-field fallback | Done |
| 3 | Wire renderer into `formatForChatDisplay()` only (NOT `toHumanReadable()`) | Done |
| 4 | Config knob `medexpertmatch.llm.response.render-embedded-json` | Done |
| 5 | `LlmResponseSanitizerTest` cases: 4 for data path, 7 for UI path | Done |
| 6 | Verify: case-analysis UI panel shows prose, server-side cache/interpretation still sees original JSON | Done |

## Acceptance criteria

- [x] `formatForChatDisplay()` of a response with `Response\n{ "requiredSpecialty": …, "icd10Codes": […], "caseSummary": … }` produces HTML with prose fields (`Recommended specialty: Urologic Oncology / Renal Cancer. Urgency: HIGH. Key findings: …. ICD-10 codes: …. Summary: …`) and **no raw JSON braces** in the chat panel
- [x] `formatForChatDisplay()` of a pure-JSON response surfaces the parsed fields instead of the generic `[Data received; unable to display formatted response]`
- [x] `toHumanReadable()` of the same response **leaves the JSON untouched** so internal consumers see the same data they always did
- [x] `toHumanReadable()` of a clean narrative returns the narrative unchanged
- [x] `toHumanReadable()` of a pure-JSON response still returns the generic fallback (data path is unchanged)
- [x] `toHumanReadable()` of a response with malformed JSON is untouched (no information loss)
- [x] `toHumanReadable()` of a response with embedded JSON in a wrapper leaves the raw JSON in place (renderer is off in the data path)
- [x] Disabling the feature via `medexpertmatch.llm.response.render-embedded-json: false` returns the original LLM text in the chat panel
- [x] `mvn test` covers all the above (11 tests) and stays green

## References

- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:401` — `toHumanReadable()` (data path, unchanged)
- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:132` — `formatForChatDisplay()` (UI path, now wires the renderer)
- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:505` — `renderEmbeddedJson()`
- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:553` — `renderJsonObjectText()`
- `src/main/java/com/berdachuk/medexpertmatch/core/config/LlmResponseRenderConfig.java` — config wiring
- `src/main/resources/application.yml` — `medexpertmatch.llm.response.render-embedded-json: true`
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentLlmSupportServiceImpl.java:125,244,396` — `toHumanReadable()` calls (data path, unchanged)
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/ChatAssistantServiceImpl.java:296` — `formatForChatDisplay()` call (UI path, now renders)
- `src/main/java/com/berdachuk/medexpertmatch/llm/harness/impl/MedicalAgentPolicyGateServiceImpl.java:49` — `formatForChatDisplay()` call (UI path, now renders)
- `src/main/java/com/berdachuk/medexpertmatch/web/service/impl/ChatMarkdownRendererImpl.java:41` — `formatForChatDisplay()` call (UI path, now renders)

