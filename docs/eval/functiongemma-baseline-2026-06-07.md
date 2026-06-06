# FunctionGemma Baseline — 2026-06-07

## Scope

M58 Phase 1 policy baseline (deterministic `ToolSelectionPolicy` + JSONL regression).  
Live `functiongemma:270m` measurement deferred to Phase 2 (requires running Ollama endpoint).

## Policy eval results

| Metric | Result |
|--------|--------|
| Cases in `tool-selection-cases.jsonl` | 53 |
| Policy regression pass rate | **100%** (`ToolSelectionEvalTest`) |
| Server-side guard | `ToolSelectionGuardingResolver` remaps `analyze_case_text` / `match_doctors_from_text` when session has case ID |

## Scenario coverage

| Scenario | Cases |
|----------|-------|
| `analyze_with_case_id_en` | 8 |
| `analyze_with_case_id_ru` | 4 |
| `match_with_case_id_en` | 8 |
| `match_follow_up_ru` | 3 |
| `route_with_case_id_en` | 3 |
| `match_from_text_no_id` | 4 |
| `analyze_from_text_no_id` | 3 |
| `evidence_pubmed` / guidelines | 6 |
| `negative_text_only` | 4 |
| Other (triage, route no ID) | 10 |

## Next steps

1. Run live baseline against Ollama `functiongemma:270m` on the same JSONL prompts.
2. Compare live accuracy to M58 thresholds (Pair A ≥ 90%, Pair B ≥ 95%).
3. If above gate, generate expanded dataset via `scripts/generate-functiongemma-training-data.py` and fine-tune.
