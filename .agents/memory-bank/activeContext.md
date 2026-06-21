# Active Context

> **GENERATED** from `records/active/*.md` + `registry/risk.jsonl`. Do not hand-edit. To start a milestone, create `records/active/M{NN}.md`; to raise a risk, append to `registry/risk.jsonl`.

## Current Focus

### M135

# M135 — Memory-Bank Enrichment & Traceability Backfill

- **Milestone:** M135
- **DEC:** DEC-015
- **Status:** Active
- **Date:** 2026-06-21

## Current Focus

Enrich the auto-generated milestone stubs (M01–M128) with richer summaries and backfill traceability links (REQ-###, TEST-###) for milestones that currently lack them.

## Tasks

1. [ ] Enrich M01–M110 stubs with structured metadata (affected modules, tests, REQ/DEC refs).
2. [ ] Mine `src/test/java/**/*IT.java` and `*Test.java` for significant test artifacts; append to `test.jsonl`.
3. [ ] Cross-check `docs/01-requirements.md` FR IDs against `registry/req.jsonl`; append missing.
4. [ ] Acquire and release a module lock in a real task to validate the flow.
5. [ ] Run `sync-memory-index.sh`, verify `--check` passes.
6. [ ] Security review.
7. [ ] Commit + merge to develop.

## Open Questions

- None.

## Risks

- RISK-135: Enrichment script may extract wrong module assignments. Mitigation: manual spot-check 10%.
## Open Questions

_Captured per-milestone in `records/active/M{NN}.md`._

## Risks

- **RISK-132** — Short-key/long-key drift in LlmResponseSanitizer (mitigated, module: core) — mitigation: dual-key fallback + parity tests
- **RISK-133** — Agents ignore do-not-hand-edit rule on generated files (mitigated, module: .agents) — mitigation: sync-memory-index.sh --check CI gate + code-style/security-check skill enforcement
- **RISK-134** — CI gate fails if jq not installed on runner (mitigated, module: .github) — mitigation: Install jq step added to ci.yml before sync check
- **RISK-135** — Enrichment script may extract wrong module assignments from archived plans (open, module: core) — mitigation: manual spot-check 10% of enriched records

## Traceability Gaps

No remaining traceability gaps.
