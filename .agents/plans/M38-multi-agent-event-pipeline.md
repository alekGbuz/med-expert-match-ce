# M38: Multi-Agent Event-Driven Pipeline

Replaces the synchronous `TaskTool` subagent mechanism with Spring Modulith event-driven agents. Each workflow phase (plan → context → execute → critic) becomes an independent agent reacting to domain events via `@ApplicationModuleListener`.

**Prerequisite:** M37 complete (see `.agents/plans/archive/M37-harness-production-hardening.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Domain events: `GoalIdentifiedEvent`, `PlanReadyEvent`, `ContextReadyEvent`, `ResultsReadyEvent`, `DoneEvent` | `feat/m38-domain-events` | ⬜ Planned | 4h |
| 2 | PlannerAgent — listens to `GoalIdentifiedEvent`, creates execution plan, emits `PlanReadyEvent` | `feat/m38-planner-agent` | ⬜ Planned | 8h |
| 3 | ContextBuilderAgent — listens to `PlanReadyEvent`, builds `CaseContextBundle`, emits `ContextReadyEvent` | `feat/m38-context-builder-agent` | ⬜ Planned | 6h |
| 4 | ExecutionAgent — listens to `ContextReadyEvent`, calls `DoctorMatchWorkflowEngine`/`RoutingWorkflowEngine`, emits `ResultsReadyEvent` | `feat/m38-execution-agent` | ⬜ Planned | 8h |
| 5 | CriticAgent — listens to `ResultsReadyEvent`, runs verify/critic, emits `DoneEvent` or re-plans | `feat/m38-critic-agent` | ⬜ Planned | 6h |
| 6 | Archive all completed plans (M08–M36) still in `.agents/plans/` | `feat/m38-archive-completed` | ⬜ Planned | 1h |

**Total effort: ~33h**

---

## D1: Domain Events

Package: `com.berdachuk.medexpertmatch.llm.event`

```java
public record GoalIdentifiedEvent(String sessionId, GoalClassification goal, String caseId, Instant timestamp) {}
public record PlanReadyEvent(String sessionId, ExecutionPlan plan, Instant timestamp) {}
public record ContextReadyEvent(String sessionId, CaseContextBundle bundle, Instant timestamp) {}
public record ResultsReadyEvent(String sessionId, MedicalAgentService.AgentResponse response, Instant timestamp) {}
public record DoneEvent(String sessionId, MedicalAgentService.AgentResponse finalResponse, Instant timestamp) {}
```

- Use Spring Modulith `ApplicationEventPublisher` for intra-module events
- Each event carries `sessionId` for trace correlation
- `ExecutionPlan` is a new record: `List<Step>` where each `Step` has `stepType`, `targetEngine`, `params`

## D2: PlannerAgent

- `@ApplicationModuleListener` on `GoalIdentifiedEvent`
- Builds `ExecutionPlan` from `GoalType`:
  - `MATCH_DOCTORS` → `[CONTEXT_BUILD, DOCTOR_MATCH, VERIFY, CRITIC]`
  - `ROUTE_CASE` → `[CONTEXT_BUILD, ROUTING, VERIFY, CRITIC]`
  - `ANALYZE_CASE` → `[CONTEXT_BUILD, ANALYSIS]`
- Persists plan via `HarnessWorkflowRunJdbcRepository`
- Emits `PlanReadyEvent`

## D3: ContextBuilderAgent

- `@ApplicationModuleListener` on `PlanReadyEvent`
- Calls existing `CaseContextBundleService.build(caseId, intent)`
- Attaches bundle to `CaseContextReady` result
- Emits `ContextReadyEvent`

## D4: ExecutionAgent

- `@ApplicationModuleListener` on `ContextReadyEvent`
- Routes to `DoctorMatchWorkflowEngine` or `RoutingWorkflowEngine` based on `ExecutionPlan`
- Emits `ResultsReadyEvent`

## D5: CriticAgent

- `@ApplicationModuleListener` on `ResultsReadyEvent`
- Calls `AgentResponseVerifier.verify()` and `MedicalAgentCriticService.review()`
- On verify/critic failure → replan (emit new `PlanReadyEvent` with retry count)
- On success → emit `DoneEvent`

## TDD

| Test | Type | What it covers |
|------|------|---------------|
| `GoalIdentifiedEventTest` | Unit | Event record creation + serialization |
| `PlannerAgentTest` | Unit | Plan construction per GoalType, event emission |
| `ContextBuilderAgentTest` | Unit | Bundle building + event chain |
| `ExecutionAgentTest` | Unit | Engine routing + ResultsReadyEvent |
| `CriticAgentTest` | Unit | Verify + critic pass/fail → replan or done |
| `MultiAgentPipelineIT` | Integration | Full event chain: GoalIdentified → DoneEvent |

---

## Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `llm/event/GoalIdentifiedEvent.java` | Domain event record |
| **NEW** | `llm/event/PlanReadyEvent.java` | Domain event record |
| **NEW** | `llm/event/ContextReadyEvent.java` | Domain event record |
| **NEW** | `llm/event/ResultsReadyEvent.java` | Domain event record |
| **NEW** | `llm/event/DoneEvent.java` | Domain event record |
| **NEW** | `llm/event/ExecutionPlan.java` | Execution plan record + Step |
| **NEW** | `llm/agent/PlannerAgent.java` | Listens GoalIdentifiedEvent → PlanReadyEvent |
| **NEW** | `llm/agent/ContextBuilderAgent.java` | Listens PlanReadyEvent → ContextReadyEvent |
| **NEW** | `llm/agent/ExecutionAgent.java` | Listens ContextReadyEvent → ResultsReadyEvent |
| **NEW** | `llm/agent/CriticAgent.java` | Listens ResultsReadyEvent → DoneEvent / replan |

---

## Acceptance Criteria

1. Full event chain: `GoalClassifier` → `GoalIdentifiedEvent` → `PlannerAgent` → `PlanReadyEvent` → `ContextBuilderAgent` → `ContextReadyEvent` → `ExecutionAgent` → `ResultsReadyEvent` → `CriticAgent` → `DoneEvent`
2. On verify/critic failure, `CriticAgent` emits replan event (not DoneEvent)
3. All 4 agents are independent `@Component` + `@ApplicationModuleListener` beans
4. Existing synchronous `processViaHarnessEngine` path remains functional as fallback
5. All tests pass

---

## Ship Order

**D1 → D2 → D3 → D4 → D5 → D6**

---

## Out of Scope

- Error-driven harness evolution with auto-GitHub issues (item 2 from `docs/harnes.md`)
- Chain replay API (M34 item 3)
- Clinician role UI parity (M34 item 4)
- Eval baseline auto-bump on green CI (M34 item 1)
