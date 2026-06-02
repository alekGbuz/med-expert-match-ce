# MedExpertMatch Harness Architecture — Current State & Proposed Improvements

## Current Harness Layer

| Harness Principle | MedExpertMatch Implementation | Status |
|---|---|---|
| **AI-layer (session wrapper)** | `ChatAssistantServiceImpl` — orchestrator LLM with system/user prompts, tool scope enforcement, session memory, stream activity | Done |
| **Task understanding** | `GoalClassifier` + LLM-based prompt (just implemented) + `ChatAgentProfile.classifyIntent()` keyword fallback | Done |
| **Planner → Coder → Critic** | `DoctorMatchWorkflowEngine` / `RoutingWorkflowEngine` — state machine: TASK_CREATED→PLANNING→CONTEXT_BUILT→TOOLS_EXECUTED→VERIFYING→CRITIC→DONE | Done |
| **Context Builder** | `CaseContextBundleService` builds PHI-safe context per `CaseContextIntent` (MATCH, ROUTE, CHAT_AUTO, ANALYZE, EVIDENCE) | Done |
| **Ralph Loop (verify → fix → retry)** | `AgentResponseVerifier` + `HarnessIterationPolicy.maxIterations` = retry on verify failure | Done |
| **Error as training signal** | `HarnessMetrics.recordVerifyFailure()` / `recordCriticFailure()` + `HarnessFailureReason` taxonomy | Partial |
| **Human-in-the-loop checkpoints** | `HarnessProperties.humanCheckpointEnabled()` → `NEEDS_HUMAN` state with checkpoint UI | Done |
| **Subagent delegation** | `TaskTool` from spring-ai-agent-utils + subagent `.md` files in `classpath:agents/` (just fixed — subagents previously had no tools) | Fixed |
| **Tool scope enforcement** | `ToolScopeEnforcingResolver` + `ChatToolContextHolder` per `ChatAgentProfile` | Done |
| **Session memory** | `SessionMemoryAdvisor` with non-LLM compaction (turn/token triggers) | Done |
| **Observability — tool calls** | `LogStreamService.logToolCall()` / `logToolResult()` / `logCompletion()` via SSE | Done |
| **Observability — task/job tracing** | Job stores (`MatchJobStore`, `RouteJobStore`, etc.) with async 202+status polling | Done |
| **Observability — harness state** | `logStreamService.sendLog(sessionId, "HARNESS_STATE", stateName)` + `HARNESS_GOAL` | Done |
| **Guarded Patch** | Not applicable (no code generation; this is a medical matching system, not a coding agent) | N/A |

## Improvements Just Implemented (Goal Identification + Engine Routing)

### Problem
The Auto orchestrator relied on the LLM to figure out which tool to call (often picking `analyze_case` instead of `match_doctors_to_case`), and the `Task` subagent ChatClient had no tools—making delegation useless.

### Solution
Three changes addressing the core harness principle: **identify goal first, then route to the right engine**.

**1. `GoalClassifier`** (`llm/chat/GoalClassifier.java`)
- LLM-based classification via `goal-classification.st` prompt → `GoalType` enum
- Fast-path keyword matching for unambiguous requests
- Fallback: if LLM fails, case ID presence → `MATCH_DOCTORS`, else `GENERAL_QUESTION`

**2. `GoalClassification`** (`llm/chat/GoalClassification.java`) + `GoalType` (`llm/chat/GoalType.java`)
- Structured goal taxonomy: `MATCH_DOCTORS`, `ANALYZE_CASE`, `ROUTE_CASE`, `TRIAGE_INTAKE`, `SEARCH_EVIDENCE`, `GENERATE_RECOMMENDATIONS`, `GENERAL_QUESTION`
- Method `isRoutableToEngine()` gates programmatic routing vs LLM chat fallback

**3. Programmatic routing** in `ChatAssistantServiceImpl.processMessage()`
- If `goal.isRoutableToEngine() && goal.hasCaseId()` → bypass LLM, call `DoctorMatchWorkflowEngine` / `RoutingWorkflowEngine` directly
- If not → fall back to LLM chat with goal-annotated user prompt

**4. Subagent tools** in `MedicalAgentConfiguration.taskTool()` — all `@Tool` components now registered on the subagent ChatClient builder, so `Task` delegation (when used) actually works.

### Flow
```
User: "Find Specialist Case Information Case ID: 6a1db20e86d74aa336e98ff0 ..."
  → GoalClassifier.classify() identifies MATCH_DOCTORS + caseId
    → processViaHarnessEngine()
      → medicalAgentService.matchDoctors(caseId, ...)
        → DoctorMatchWorkflowEngine.execute()
          → TASK_CREATED → PLANNING → CONTEXT_BUILT → TOOLS_EXECUTED → VERIFYING → CRITIC → DONE
          → Returns ranked doctors with scores + narrative
```

### Prompt Improvements
Updated three prompts to eliminate broken `Task` delegation instructions:
- `chat-agent-orchestrator-instructions.st` — explicit: "for doctor matching → call match_doctors_to_case, for analysis → call analyze_case"
- `chat-agent-system.st` — rules now distinguish matching vs analysis tools
- `goal-classification.st` — new prompt for LLM goal classification with priority rules

## Proposed Further Improvements

### 1. Multi-Agent Workflow (Phase 2)
Replace the `TaskTool` subagent mechanism with native Spring Modulith event-driven pipeline:
```
GoalClassifier → GoalIdentifiedEvent
  → PlannerAgent (plan steps) → PlanReadyEvent
    → ContextBuilderAgent (build bundle) → ContextReadyEvent
      → ExecutionAgent (match_doctors_to_case) → ResultsReadyEvent
        → CriticAgent (review) → DoneEvent
```
State machine managed by `WorkflowStateStore` (JDBC), each agent is a separate session with its own model call.

### 2. Error-Driven Harness Evolution
Build `HarnessImprovementService`:
- Periodically analyze `HarnessFailureReason` distribution from `HarnessMetrics`
- Auto-create GitHub issues for top failure patterns with suggested improvements
- Example: "30% of MATCH_DOCTORS failures are ITERATION_LIMIT — increase maxIterations or add early-exit signal"

### 3. Context Strategy per Goal
Different goals need different context bundles:
- `MATCH_DOCTORS` → case summary + ICD-10 codes + specialty + PHI-anonymized abstract
- `ANALYZE_CASE` → full clinical details + patient age + symptoms
- `ROUTE_CASE` → geographic filters + urgency + required capabilities

Currently `CHAT_AUTO` intent is used for all chat contexts; add per-goal `CaseContextIntent` variants.

### 4. Streaming Harness Events to Chat UI
Currently harness state transitions (`HARNESS_STATE` events) are logged via SSE already, but the chat UI doesn't show a progress timeline. Surface:
- What engine is running (DoctorMatch vs Routing)
- Current state (PLANNING / TOOLS_EXECUTED / DONE)
- Verification result (passed/failed)
- Critic result (approved/needs-fix)

### 5. Hybrid Routing: LLM Fallback When Engine Fails
If the harness engine returns no results (0 doctors matched), auto-fallback to LLM chat with a context bundle explaining "no matches from GraphRAG, suggest alternative search criteria."

