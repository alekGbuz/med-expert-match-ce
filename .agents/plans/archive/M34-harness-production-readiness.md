# M34: Harness Production Readiness

Hardening harness for production operators: live eval baseline updates, chain replay, and alert wiring.

**Prerequisite:** M33 complete (see `.agents/plans/archive/M33-harness-full-eval-and-chain-ui.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Live eval run updates `baseline-pass-rate.txt` on green CI | `feat/m34-eval-baseline-bump` | ⬜ Planned | 6h |
| 2 | Grafana alert on harness verify failure rate spike | `feat/m34-harness-alerts` | ⬜ Planned | 4h |
| 3 | Chain replay API (re-trigger handoff from trace) | `feat/m34-chain-replay` | ⬜ Planned | 8h |
| 4 | Clinician role UI parity for harness admin pages | `feat/m34-clinician-ui` | ⬜ Planned | 4h |
| 5 | Harness run retention + purge job | `feat/m34-run-retention` | ⬜ Planned | 6h |
| 6 | End-to-end harness IT (intake→match→recommend chain) | `feat/m34-e2e-harness-it` | ⬜ Planned | 8h |

**Total effort: ~36h**

---

## References

- M33 archive: `.agents/plans/archive/M33-harness-full-eval-and-chain-ui.md`
