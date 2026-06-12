# M95: LLM Prompt Quality and Response Reliability

**Status:** Active (planned 2026-06-12)
**Created:** 2026-06-12
**Depends on:** M94 (archived)

## Problem Statement

The case analysis workflow produces unreliable LLM output with `medgemma1.5:4b`:

1. **LLM hallucinates ICD-10 codes** — case `I50.9` gets expanded to `I25.9, N30.0` or `I50.9, N20.30` or `I50.9, D27.1` — codes not present in the original case data.
2. **LLM produces meta-text instead of analysis** — the model sometimes outputs apologetic text ("My apologies, I seem to have misread the request...") instead of the requested case description.
3. **High latency, low output** — 62s latency for only 102 tokens in one run, suggesting the model struggles with the prompt structure.
4. **Urgency inconsistency** — the same case gets `HIGH` from the match workflow but `MEDIUM` from the case analysis workflow.

## Goal

1. Audit and simplify the case analysis interpretation prompt (`medgemma-case-analysis-interpretation-system.st`) to reduce model confusion.
2. Add explicit output format instructions (JSON or structured template) to constrain LLM output.
3. Reduce prompt token count to improve latency and reliability.
4. Add fallback/validation for ICD-10 codes — only use codes from the original case data, never from LLM output.
5. `mvn verify` green.
6. Archive plan.

## Acceptance Criteria

- [ ] Case analysis interpretation prompt simplified and tested
- [ ] ICD-10 codes in output come only from original case data (not LLM)
- [ ] Urgency level is consistent between match and analysis workflows
- [ ] `mvn verify` exits 0

## References

- `src/main/resources/prompts/medgemma-case-analysis-interpretation-system.st` — interpretation prompt
- `src/main/resources/prompts/medgemma-case-analysis-system.st` — case analysis prompt
- `src/main/java/.../llm/service/impl/MedicalAgentCaseAnalysisWorkflowServiceImpl.java` — case analysis workflow
- `src/main/java/.../llm/service/impl/MedicalAgentLlmSupportServiceImpl.java` — LLM support service