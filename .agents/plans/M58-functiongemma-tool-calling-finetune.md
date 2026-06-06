# M58: FunctionGemma Tool-Calling Fine-Tune (Optional)

**Status:** Phase 1 complete (policy eval + server-side guard); Phase 2–4 pending live baseline / fine-tune  
**Created:** 2026-05-31  
**Depends on:** M57 (goal-classifier-hybrid-session-routing) — complete session routing before measuring tool-selection gaps.

## Phase progress

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Policy eval JSONL, `ToolSelectionEvalTest`, `ToolSelectionGuardingResolver`, training data script | **Done** (2026-06-07) |
| 2 | Live golden eval + before/after compare (`tool-selection-golden.jsonl`, `ToolSelectionLiveEvalIT`, scripts) | **Done** (2026-06-07) — run manually against Ollama |
| 3 | Fine-tune (Tuning Lab or TRL) | Pending |
| 4 | Serve fine-tuned model + smoke | Pending |

## Problem Statement

`functiongemma:270m` (`spring.ai.custom.tool-calling.model`) powers Auto orchestrator tool-calling in AI Chat. Even when `ChatCasePromptSupport` injects a 24-character case ID into the user prompt, the model sometimes:

- Responds with natural language instead of a function call
- Chooses `analyze_case_text` when `analyze_case(caseId)` is correct
- Chooses `match_doctors_from_text` when `match_doctors_to_case(caseId)` is correct

This matches Google's documented baseline behavior: generic FunctionGemma may **discuss** policy instead of executing the right tool ([Fine-tuning guide](https://developers.googleblog.com/a-guide-to-fine-tuning-functiongemma/), [FunctionGemma announcement](https://blog.google/innovation-and-ai/technology/developers-tools/functiongemma/)).

**Important:** Fine-tuning solves **tool disambiguation**, not harness goal routing. M57 must land first.

## When to Start M58

**Gate criteria** — proceed only if after M57:

| Signal | Threshold |
|--------|-----------|
| Residual «ask for case text» with case ID in hints | > 5% of analyze turns in manual eval |
| Wrong tool selected (text vs caseId variant) | > 10% on tool-selection eval set |
| M57 harness route for `ANALYZE_CASE` not enabled | FunctionGemma still primary for analyze |

If M57 phase 4 (harness analyze) covers most analyze turns, M58 priority drops to **P3**.

## Goal

Fine-tune FunctionGemma on MedExpertMatch-specific tool schemas so it reliably selects the correct tool and arguments when case context is present in the prompt — including Russian and English user messages.

## Non-Goals

- Fine-tuning for goal classification (`MATCH_DOCTORS` vs `ANALYZE_CASE`) — use medgemma + M57 rules
- Changing `pom.xml` model versions
- On-device / edge deployment
- Replacing medgemma for case analysis or interpretation inside harness

## Tool Pairs to Optimize

Primary ambiguity pairs (highest ROI):

| Pair | Wrong choice | Correct choice | Trigger |
|------|--------------|----------------|---------|
| A | `analyze_case_text` | `analyze_case` | Case ID in `IMPORTANT — medical case ID` hint |
| B | `match_doctors_from_text` | `match_doctors_to_case` | Case ID in hint + match intent |
| C | `analyze_case` | `match_doctors_to_case` | User asks analyze/detail, not doctors |
| D | text reply | any tool | User message is actionable, hints present |

Secondary:

| Pair | Notes |
|------|-------|
| `search_clinical_guidelines` vs `query_pubmed` | Evidence intent |
| `match_facilities_for_case` vs `match_doctors_to_case` | Route vs match |

## Expected Benefits

Based on Google benchmarks (Mobile Actions held-out eval):

| Metric | Base FunctionGemma | After fine-tune (expected) | MedExpertMatch applicability |
|--------|-------------------|---------------------------|------------------------------|
| Tool selection accuracy | ~58% (Google baseline) | **80–88%** | Pairs A/B/C in our orchestrator |
| «Discuss instead of call» rate | high on policy-like prompts | **−60–70%** | «provide case description» failures |
| RU+EN mixed prompts | variable | improved (256k vocab, JSON tokenization) | RU chat users |
| Latency | unchanged | unchanged | Same 270M model |
| Cost | baseline | +0 at inference | One-time training cost |

**Conservative internal targets** (measure on our eval set before/after):

| KPI | Baseline (estimate) | Post fine-tune target |
|-----|---------------------|----------------------|
| `analyze_case` when caseId in hint | ~40–60% | **≥ 90%** |
| `match_doctors_to_case` when caseId in hint | ~70–80% | **≥ 95%** |
| Spurious `AskUserQuestion` / text-only | ~15–25% | **≤ 5%** |
| End-to-end «детализируй случай» success (if not harness-routed) | low | **≥ 85%** |

Fine-tuning does **not** improve: GraphRAG match quality, harness verify logic, or case ID extraction — only orchestrator tool choice.

## Dataset Design

### Format

Follow FunctionGemma conversational SFT format per [Google AI fine-tuning guide](https://ai.google.dev/gemma/docs/functiongemma/finetuning-with-functiongemma):

Each training example = multi-turn conversation:

```
System: {orchestrator instructions — truncated stable prefix}
User: {caseToolHints + goalHint + userMessage}
Assistant: <start_function_call>call:analyze_case{caseId:<escape>6a23f05200155d711484cf69<escape>}<end_function_call>
```

For CSV / Tuning Lab import:

| Column | Description |
|--------|-------------|
| `user_prompt` | Full rendered user message (hints + goal + message) |
| `tool_name` | e.g. `analyze_case` |
| `tool_args_json` | `{"caseId":"6a23f05200155d711484cf69"}` |
| `locale` | `en` / `ru` |
| `scenario` | `analyze_with_case_id`, `match_with_case_id`, `match_follow_up_exclude`, etc. |

### Size & split

| Set | Examples | Notes |
|-----|----------|-------|
| Train | **400–600** | Balanced across pairs A–D, 40% RU |
| Validation | **80–100** | Held-out scenarios |
| Test | **80–100** | `shuffle=True`, never seen tool combos |

**Critical:** Pre-mix categories before split ([Google warning on sorted data](https://developers.googleblog.com/a-guide-to-fine-tuning-functiongemma/)). Use `shuffle=True`, stratify by `scenario`.

### Data sources

1. **Synthetic generation** — script renders prompts from `chat-agent-orchestrator-instructions.st` + `chat-case-id-hint.st` + message templates (no PHI)
2. **Log mining** — anonymize production logs where tool choice was wrong (manual label)
3. **Failure replay** — cases from M57 eval JSONL that reached FunctionGemma incorrectly

Example scenarios (minimum 20 examples each):

```
analyze_with_case_id_en    → analyze_case
analyze_with_case_id_ru    → analyze_case
match_with_case_id_en      → match_doctors_to_case
match_follow_up_ru         → match_doctors_to_case + excludePreviouslyMatched
match_from_text_no_id      → match_doctors_from_text
analyze_from_text_no_id    → analyze_case_text
evidence_pubmed              → query_pubmed
negative_text_only         → (no tool — rare, <5% of set)
```

### Negative examples

Include ~5% examples where **no tool** is correct (general medical Q without case) to avoid over-calling tools.

## Training Approach

### Option A — FunctionGemma Tuning Lab (recommended for first iteration)

- Hugging Face Space: [FunctionGemma Tuning Lab](https://developers.googleblog.com/a-guide-to-fine-tuning-functiongemma/)
- Upload CSV, define tool schemas matching `CaseAnalysisAgentTools`, `DoctorMatchingAgentTools`
- Defaults: tune learning rate / epochs via UI; monitor loss curve
- Export fine-tuned weights → serve via Ollama/LM Studio OpenAI-compatible API

**Effort:** ~2–3 days (dataset + train + smoke test)

### Option B — TRL SFTTrainer (reproducible, CI-friendly)

Follow [fine-tuning-with-functiongemma](https://ai.google.dev/gemma/docs/functiongemma/finetuning-with-functiongemma):

- Base model: `google/functiongemma-270m` (or current Ollama tag)
- `SFTTrainer`, 6–8 epochs (start), batch size per GPU
- Store adapter or merged weights in `models/functiongemma-medexpertmatch/` (gitignored)
- Document serve command for local profile

**Effort:** ~4–5 days

### Serving integration

**Files (config only, no pom version bumps):**

```yaml
# application-local.yml
TOOL_CALLING_MODEL: functiongemma-medexpertmatch:270m  # or HF model id
```

`SpringAIConfig.toolCallingChatModel()` already reads `spring.ai.custom.tool-calling.model`.

Add health check: `ComprehensiveHealthIndicator` reports fine-tuned model name.

## Evaluation Protocol

### Pre-training baseline

1. Freeze current `functiongemma:270m`
2. Run `ToolSelectionEvalTest` (new) — 100 scripted prompts, assert tool name + args
3. Record baseline metrics in `docs/eval/functiongemma-baseline-{date}.md`

### Post-training

1. Same 100 prompts → compare accuracy
2. Manual smoke: 10 real chat flows (Find Specialist → detail → more doctors)
3. Regression: ensure non-case general questions still work

### Acceptance criteria

- [x] Policy eval JSONL + `ToolSelectionEvalTest` (53 cases, 100% pass)
- [x] Server-side tool guard (`ToolSelectionGuardingResolver`)
- [x] Synthetic training data script (`scripts/generate-functiongemma-training-data.py`)
- [x] Golden live eval dataset (`tool-selection-golden.jsonl`) + `run-tool-selection-live-eval.sh` + compare script
- [ ] Pair A live accuracy ≥ 90% (measure via live golden eval)
- [ ] Pair B live accuracy ≥ 95%
- [ ] No regression on `analyze_case_text` when **no** case ID in hints (live model)
- [ ] `mvn verify` green with default (base) model in CI; fine-tuned model tested in optional profile `local-finetuned`

## Repository Artifacts (M58 deliverables)

| Artifact | Location |
|----------|----------|
| Dataset spec | `.agents/plans/M58-dataset-spec.md` (optional child doc) |
| Synthetic data script | `scripts/generate-tool-selection-eval-dataset.py` (+ `tool_selection_dataset_lib.py`) |
| Unsloth export | `scripts/export-unsloth-functiongemma-dataset.py` + `medexpertmatch_tool_schemas.json` |
| Eval JSONL | `src/test/resources/eval/tool-selection-cases.jsonl` |
| Eval test | `src/test/java/.../llm/eval/ToolSelectionEvalTest.java` |
| Training notebook / Colab link | `docs/ai/functiongemma-finetune.md` |
| Baseline vs fine-tuned report | `docs/eval/functiongemma-baseline-*.md` |

**Do not commit:** model weights, training checkpoints, API keys.

## Operational Considerations

| Topic | Guidance |
|-------|----------|
| Re-training trigger | Tool schema change, new agent tools, new language |
| Versioning | Tag model `functiongemma-medexpertmatch-v1`, document in README |
| Rollback | Keep base `functiongemma:270m` as fallback via env var |
| HIPAA | Training data synthetic only; no PHI from prod logs without anonymization review |
| CI | CI uses base model; fine-tuned eval in manual/nightly job |

## Effort Estimate

| Task | Effort |
|------|--------|
| Dataset design + synthetic generation | 1.5 days |
| Baseline eval + tooling | 1 day |
| Fine-tune (Lab or TRL) | 1–2 days |
| Integrate + smoke + document | 1 day |
| **Total** | **4.5–5.5 days** |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Overfits to caseId always → analyze | Balance with no-id text examples |
| Breaks general chat | 5% negative examples; A/B with base model |
| Ollama model name drift | Pin digest / manifest |
| Maintenance burden | Only pursue if M57 residual error rate justifies it |
| Fine-tune helps tool choice but not bad goals | M57 gate mandatory |

## Decision Matrix

| Condition | Recommendation |
|-----------|----------------|
| M57 harness analyze enabled + stable | **Defer M58** — monitor 2 weeks |
| Residual tool errors > 10% after M57 | **Proceed M58 Option A** |
| Need reproducible pipeline | **Option B TRL** |
| Tool schema changes frequently | Prefer M57 server-side tool guards over fine-tune |

## Success Metrics Summary

```
Before M57 + M58:
  «детализируй клинический случай» → GENERAL_QUESTION → text ask ❌

After M57 only:
  → ANALYZE_CASE + caseId → harness analyze ✅ (target)

After M57 + M58 (if needed):
  → ANALYZE_CASE + FunctionGemma path → analyze_case tool ≥90% ✅
```

## References

- [A Guide to Fine-Tuning FunctionGemma](https://developers.googleblog.com/a-guide-to-fine-tuning-functiongemma/)
- [FunctionGemma: Bringing bespoke function calling to the edge](https://blog.google/innovation-and-ai/technology/developers-tools/functiongemma/)
- [Fine-tuning with FunctionGemma (Google AI)](https://ai.google.dev/gemma/docs/functiongemma/finetuning-with-functiongemma)
- Internal: M57 goal classifier hybrid plan
