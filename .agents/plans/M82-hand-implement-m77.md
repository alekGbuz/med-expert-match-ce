# M82: Hand-Implement M77 (Run the M77-Stories in `--agent stub` Mode)

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M79 (done — `M77-stories.json` enumerates 10 atomic stories), M81 (in progress — the unattended pilot; M82 is the hand-driven fallback for when the pilot is not run, fails, or only partially completes), the archived M77 plan (`.agents/plans/archive/M77-runtime-measured-estimates.md`) for the M77 feature spec.

## Problem Statement

M77 is the "live-measured synthetic data estimates" feature. Its 10 stories are enumerated in `.agents/plans/M77-stories.json` and correspond 1:1 to the 10 Phases in the archived `M77-runtime-measured-estimates.md:64-75` plan. The feature has not been implemented yet.

M81 is the unattended Ralph pilot that *might* implement M77 in one shot, but it requires a real `OPENAI_API_KEY` and 6 hours of wall-clock. Until the pilot runs, the feature remains unbuilt. Even if M81 runs, it could end in state B (partial: some stories green, some red), and a human still has to drive the red ones to green.

M82 is the hand-driven path: a human implements the 10 M77 stories in their own working tree, marking each green via `./scripts/ralph.sh M77 --agent stub` so the loop still does the test+commit+mark+progress.txt bookkeeping. M82 is the fallback for every case where the pilot is not the right tool (no API key, partial pilot, time-boxed hand-driven session).

The M77 work itself is non-trivial: a new table (`synthetic_data_generation_runs`), a new repository, run tracking wired into 3 phase methods, a nightly `@Scheduled` job that mutates `data-sizes.csv`, a UI template change, and integration tests. The full spec is in the archived M77 plan; M82 just points at it.

## Goal

1. **Implement M77-01..M77-10** in order (priority 1 through 10), in the human's working tree on `feature/m82-hand-implement-m77`, with `./scripts/ralph.sh M77 --agent stub` doing the test+commit+mark+progress.txt loop on each.
2. **Each story's `test_target` is the gate**: the loop will only mark `passes: true` when the test is green. M77-09 (`mvn verify green`) and M77-10 (24h manual smoke) are themselves stories and behave the same way.
3. **When all 10 stories are `passes: true`**, M77 is done. Merge `feature/m82-hand-implement-m77` to develop. Archive the M77 plan (already archived) and this M82 plan.
4. **If M81 has already run** (and a `feature/m81-ralph-pilot-m77` branch exists with some green commits), cherry-pick the green M77 stories from the pilot branch into `feature/m82-hand-implement-m77` to avoid redoing the pilot's work. The pilot's red blocks in `progress.txt` are the lessons-learned record; carry them forward.

## Non-Goals

| Don't | Why |
|---|---|
| Run the unattended pilot | M81's scope |
| Modify `M77-stories.json` to make M77 easier | the story contract is the spec; difficulty is the difficulty |
| Skip M77-09 or M77-10 | they are stories too; even M77-10's 24h wait is documented in `progress.txt` as a "next" pointer |
| Implement M77 features not in the archived M77 plan | M77's scope is fixed; new features are M83+ |
| Touch HIPAA / auth / V2+ migrations / pom.xml | global boundaries |

## Changes

### Part 1 — The implementation

| Area | File | Change |
|------|------|--------|
| New feature branch | `feature/m82-hand-implement-m77` (created from develop) | All 10 story commits land here. |
| For each story M77-0N | per the M77-stories.json `files_touched[]` and `accept[]` | The human implements the change in their working tree, then `./scripts/ralph.sh M77 --agent stub` runs the test, commits (no `Co-authored-by:` trailer), and marks `passes: true`. The loop is the bookkeeping; the human is the implementation. |
| M77-stories.json | (untouched) | The loop updates `passes` and `commit_sha`; the human never edits the file by hand. |

### Part 2 — Pilot integration (only if M81 already ran)

| Area | File | Change |
|------|------|--------|
| Cherry-pick | `git cherry-pick <pilot-green-sha>...` | If `feature/m81-ralph-pilot-m77` exists with green commits, cherry-pick them onto `feature/m82-hand-implement-m77` in story-id order. The cherry-pick may have conflicts (different bases) that the human resolves. |
| Pilot lessons | `progress.txt` (carried forward) | The pilot's `[RED]` and `[RED-AGENT]` blocks stay in `progress.txt` as a record of what Ralph tried. They are not redone. |

### Part 3 — Tests

The M77 plan already defines 4 test classes (Phases 1, 4, 7, and the mvn-verify Phase 9). The loop's `test_target` field in `M77-stories.json` already points at each. No new test surface in M82.

| Story | Test target |
|-------|-------------|
| M77-01 | `SyntheticDataGenerationRunRepositoryTest` |
| M77-02 | `SyntheticDataGenerationRunRepositoryTest` (turns green) |
| M77-03 | `SyntheticDataPostProcessingServiceImplRunTrackingTest` |
| M77-04 | `EstimateAdjustmentServiceTest` |
| M77-05 | `EstimateAdjustmentServiceTest` (turns green) |
| M77-06 | `SyntheticDataGenerationProgressServiceImplTest` |
| M77-07 | `SyntheticDataControllerStateIT` |
| M77-08 | `SyntheticDataControllerStateIT` (no API change, just template) |
| M77-09 | `mvn verify` (whole suite) |
| M77-10 | (manual smoke, no test) |

### Part 4 — Documentation

| Area | File | Change |
|------|------|--------|
| `00-index.md` | (no change yet) | When M82 finishes, archive M82 + M77 (already archived) and update the index. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Create `feature/m82-hand-implement-m77` from develop. Optional: cherry-pick green M81 pilot commits if they exist. | Pending |
| 2 | Hand-implement M77-01 (`SyntheticDataGenerationRun` record + repository + Flyway V1 migration). Run `./scripts/ralph.sh M77 --agent stub` to test+commit+mark. | Pending |
| 3 | Hand-implement M77-02..M77-03 (wire run tracking into 3 phase methods). Each ralph invocation. | Pending |
| 4 | Hand-implement M77-04..M77-05 (`EstimateAdjustmentService` + `@Scheduled` + CSV writer). Each ralph invocation. | Pending |
| 5 | Hand-implement M77-06 (wire `recentRunsBySize` into progress service). | Pending |
| 6 | Hand-implement M77-07 (IT for `/state?jobId=...` returning run history). | Pending |
| 7 | Hand-implement M77-08 (template: "Last actual" line). | Pending |
| 8 | Hand-implement M77-09 (`mvn verify` green). | Pending |
| 9 | Hand-implement M77-10 (manual smoke: 24h wait for nightly job). | Pending |
| 10 | `mvn verify` green (regression check). | Pending |
| 11 | All 10 stories `passes: true` in `M77-stories.json`. | Pending |
| 12 | Merge `feature/m82-hand-implement-m77` to develop, archive this M82 plan, update `00-index.md`. | Pending |

## Acceptance criteria

- [ ] `M77-stories.json` shows all 10 stories with `passes: true` and a `commit_sha`
- [ ] `progress.txt` shows 10 `[GREEN]` blocks (one per story), each with the commit SHA
- [ ] `mvn verify` exits 0 with all pre-existing tests + the 4 new M77 test classes passing
- [ ] The admin UI shows "Estimated: 5 minutes" AND "Last actual: 1 min 35 s" below the size selector (per M77 acceptance)
- [ ] `data-sizes.csv` has a `large` row with `estimated_time_minutes = 3` after the first nightly job runs (or will, after 24h)
- [ ] No regressions: M73/M74/M75/M76/M79/M80/M81 contracts are unchanged
- [ ] No `Co-authored-by:` trailers in any of the 10 commits

## Out of scope

- Building a real-time progress chart for the M77 `recentRunsBySize` (M70's agent activity panel already covers this for humans)
- Adjusting `data-sizes.csv`'s `case_count` column dynamically (M77 only auto-adjusts `estimated_time_minutes`)
- Modifying the Ralph loop (M82 consumes the loop as-is)

## References

- `.agents/plans/archive/M77-runtime-measured-estimates.md` — the M77 spec; the source of the 10 stories
- `.agents/plans/M77-stories.json` — the 10 atomic stories with `test_target`, `files_touched[]`, `accept[]`, `skills_to_load[]`
- `.agents/plans/M81-run-ralph-pilot-on-m77.md` — the automated pilot attempt (M82 is its hand-driven fallback)
- `.agents/skills/domain-modeling/SKILL.md`, `.agents/skills/db-migrations/SKILL.md`, `.agents/skills/testing/SKILL.md` — load as `skills_to_load[]` per story
- `AGENTS.md:117-135` — global boundaries (no HIPAA, no PHI, no V2+ migrations, no AI provider swap, no `pom.xml` changes)
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataPostProcessingServiceImpl.java` — the 3 phase methods that get run tracking wired in (M77-03)
- `src/main/resources/data/data-sizes.csv` — the CSV that the nightly `@Scheduled` job mutates (M77-05)
- `src/main/resources/templates/admin/synthetic-data.html:168-213` — the time-estimate display that gains a "Last actual" line (M77-08)
