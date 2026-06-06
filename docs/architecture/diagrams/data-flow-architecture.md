# Data Flow Architecture

This diagram shows the primary data flows between the Chat Layer, Harness Layer, Policy Layer, and Data Layer in `med-expert-match-ce`, including safety checks and exception paths.

```mermaid
%%{init: {
  "flowchart": {
    "htmlLabels": true,
    "curve": "basis"
  },
  "securityLevel": "loose",
  "theme": "base"
}}%%

flowchart TD

%% =========================
%% ENTRY
%% =========================
USER([User / Clinician / Case Manager])
CLIENT[Client UI<br/>Chat, Case Form, Expert Search]
GW[API Gateway / Edge<br/>TLS, rate limit, request size limit]

USER --> CLIENT --> GW

%% =========================
%% CHAT LAYER
%% =========================
subgraph CHAT["Chat Layer"]
  CL_AUTH{AuthN/AuthZ<br/>valid session?}
  CL_INPUT[Input Normalizer<br/>locale, schema, message type]
  CL_SAFE[Prompt Safety Gate<br/>PII/PHI boundary, jailbreak, injection]
  CL_CASE[Case Intake Mapper<br/>symptoms, diagnosis, region, urgency]
  CL_CTX[Conversation Context Manager<br/>short-term memory, turn state]
  CL_CONSENT{Consent / Purpose Check<br/>medical data allowed?}
  CL_RESPONSE[Response Composer<br/>explainable answer, citations, next action]
end

GW --> CL_AUTH
CL_AUTH -->|ok| CL_INPUT
CL_AUTH -->|fail| EX_AUTH

CL_INPUT --> CL_SAFE
CL_SAFE -->|safe| CL_CONSENT
CL_SAFE -->|unsafe / prompt injection| EX_SECURITY

CL_CONSENT -->|allowed| CL_CASE
CL_CONSENT -->|missing consent| EX_CONSENT

CL_CASE --> CL_CTX

%% =========================
%% HARNESS LAYER
%% =========================
subgraph HARNESS["Harness Layer"]
  HL_ROUTE[Agent Router<br/>case-understanding, expert-match, clarification]
  HL_PLAN[Task Planner<br/>steps, tools, constraints]
  HL_GUARD[Tool Invocation Guard<br/>allowed tools, argument validation]
  HL_EXEC[Agent Executor<br/>Spring AI agentic pipeline]
  HL_RETRY{Retry / Fallback Policy<br/>transient failure?}
  HL_TRACE[Trace Collector<br/>correlation id, spans, audit events]
  HL_RESULT[Candidate Result Aggregator<br/>ranked experts, rationale, evidence links]
end

CL_CTX --> HL_ROUTE
HL_ROUTE --> HL_PLAN
HL_PLAN --> HL_GUARD
HL_GUARD -->|approved| HL_EXEC
HL_GUARD -->|tool denied / invalid args| EX_TOOL_DENIED

HL_EXEC --> HL_TRACE
HL_EXEC --> HL_RETRY
HL_RETRY -->|retryable| HL_EXEC
HL_RETRY -->|non-retryable| EX_AGENT_FAILURE

%% =========================
%% POLICY LAYER
%% =========================
subgraph POLICY["Policy Layer"]
  PL_POLICY[Policy Engine<br/>medical domain rules, product constraints]
  PL_EVIDENCE{Evidence Gate<br/>enough grounded evidence?}
  PL_RISK{Clinical Risk Gate<br/>high-risk / emergency?}
  PL_BIAS[Fairness / Bias Check<br/>location, specialty, availability]
  PL_PRIVACY[Privacy Filter<br/>minimum necessary disclosure]
  PL_EXPLAIN[Explainability Gate<br/>why these experts, confidence, limits]
  PL_APPROVE{Final Policy Approval<br/>safe to answer?}
end

HL_EXEC --> PL_POLICY
PL_POLICY --> PL_EVIDENCE
PL_EVIDENCE -->|pass| PL_RISK
PL_EVIDENCE -->|fail| EX_LOW_EVIDENCE

PL_RISK -->|routine| PL_BIAS
PL_RISK -->|urgent / unsafe| EX_MEDICAL_ESCALATION

PL_BIAS --> PL_PRIVACY
PL_PRIVACY --> PL_EXPLAIN
PL_EXPLAIN --> PL_APPROVE

PL_APPROVE -->|approved| HL_RESULT
PL_APPROVE -->|blocked| EX_POLICY_BLOCK

HL_RESULT --> CL_RESPONSE
CL_RESPONSE --> CLIENT

%% =========================
%% DATA LAYER
%% =========================
subgraph DATA["Data Layer"]
  DL_ACCESS[Data Access Facade<br/>repository boundary, tenant checks]
  DL_CASE[(PostgreSQL<br/>case data, user state)]
  DL_VECTOR[(PgVector<br/>embeddings, semantic retrieval)]
  DL_GRAPH[(Apache AGE / GraphRAG<br/>expert graph, specialties, relations)]
  DL_AUDIT[(Audit Log<br/>immutable security and decision events)]
  DL_CACHE[(Cache<br/>safe metadata only)]
  DL_SECRET[Secrets / Config Provider<br/>tokens, model endpoints]
end

HL_GUARD --> DL_ACCESS
PL_POLICY --> DL_ACCESS
HL_TRACE --> DL_AUDIT
CL_AUTH --> DL_AUDIT
PL_APPROVE --> DL_AUDIT

DL_ACCESS --> SEC_TENANT{Tenant / Ownership Check}
SEC_TENANT -->|ok| SEC_DATA_CLASS
SEC_TENANT -->|fail| EX_DATA_ACCESS

SEC_DATA_CLASS{Data Classification Check<br/>PHI, PII, public metadata}
SEC_DATA_CLASS -->|case data| DL_CASE
SEC_DATA_CLASS -->|semantic search| DL_VECTOR
SEC_DATA_CLASS -->|expert graph| DL_GRAPH
SEC_DATA_CLASS -->|cacheable metadata| DL_CACHE
SEC_DATA_CLASS -->|restricted secret| DL_SECRET
SEC_DATA_CLASS -->|policy violation| EX_DATA_ACCESS

DL_CASE --> HL_EXEC
DL_VECTOR --> HL_EXEC
DL_GRAPH --> HL_EXEC
DL_CACHE --> HL_EXEC
DL_SECRET --> HL_EXEC

%% =========================
%% EXCEPTION HANDLING
%% =========================
subgraph EXCEPTIONS["Exception / Safety Handling"]
  EX_AUTH[401 / 403<br/>reject request, audit event]
  EX_CONSENT[Consent Required<br/>ask for explicit consent or reduce scope]
  EX_SECURITY[Security Block<br/>jailbreak, injection, unsafe PHI handling]
  EX_TOOL_DENIED[Tool Denied<br/>stop tool call, log policy reason]
  EX_AGENT_FAILURE[Agent Failure<br/>fallback answer, incident signal]
  EX_LOW_EVIDENCE[Low Evidence<br/>ask clarification or say insufficient evidence]
  EX_MEDICAL_ESCALATION[Medical Escalation<br/>urgent-care disclaimer, human handoff]
  EX_POLICY_BLOCK[Policy Block<br/>safe refusal, explain limitation]
  EX_DATA_ACCESS[Data Access Violation<br/>deny, audit, alert]
  EX_OBS[Observability / Alerting<br/>metrics, traces, security alerts]
end

EX_AUTH --> EX_OBS
EX_CONSENT --> EX_OBS
EX_SECURITY --> EX_OBS
EX_TOOL_DENIED --> EX_OBS
EX_AGENT_FAILURE --> EX_OBS
EX_LOW_EVIDENCE --> EX_OBS
EX_MEDICAL_ESCALATION --> EX_OBS
EX_POLICY_BLOCK --> EX_OBS
EX_DATA_ACCESS --> EX_OBS

EX_AUTH --> CLIENT
EX_CONSENT --> CLIENT
EX_SECURITY --> CLIENT
EX_AGENT_FAILURE --> CLIENT
EX_LOW_EVIDENCE --> CL_RESPONSE
EX_MEDICAL_ESCALATION --> CL_RESPONSE
EX_POLICY_BLOCK --> CL_RESPONSE
EX_DATA_ACCESS --> CLIENT

EX_OBS --> DL_AUDIT

%% =========================
%% STYLES
%% =========================
classDef chat fill:#E8F3FF,stroke:#2274A5,stroke-width:1.5px,color:#0B2942;
classDef harness fill:#EEF7E8,stroke:#4A8F29,stroke-width:1.5px,color:#17320E;
classDef policy fill:#FFF4D6,stroke:#C28A00,stroke-width:1.5px,color:#3D2B00;
classDef data fill:#F2ECFF,stroke:#6B46C1,stroke-width:1.5px,color:#261447;
classDef exception fill:#FFE8E8,stroke:#C53030,stroke-width:1.5px,color:#4A0E0E;
classDef security fill:#FFF0F6,stroke:#B83280,stroke-width:1.5px,color:#3B0A24;
classDef actor fill:#F7FAFC,stroke:#2D3748,stroke-width:1.5px,color:#1A202C;

class USER,CLIENT,GW actor;
class CL_AUTH,CL_INPUT,CL_SAFE,CL_CASE,CL_CTX,CL_CONSENT,CL_RESPONSE chat;
class HL_ROUTE,HL_PLAN,HL_GUARD,HL_EXEC,HL_RETRY,HL_TRACE,HL_RESULT harness;
class PL_POLICY,PL_EVIDENCE,PL_RISK,PL_BIAS,PL_PRIVACY,PL_EXPLAIN,PL_APPROVE policy;
class DL_ACCESS,DL_CASE,DL_VECTOR,DL_GRAPH,DL_AUDIT,DL_CACHE,DL_SECRET data;
class SEC_TENANT,SEC_DATA_CLASS security;
class EX_AUTH,EX_CONSENT,EX_SECURITY,EX_TOOL_DENIED,EX_AGENT_FAILURE,EX_LOW_EVIDENCE,EX_MEDICAL_ESCALATION,EX_POLICY_BLOCK,EX_DATA_ACCESS,EX_OBS exception;

%% =========================
%% INTERACTIVE LINKS
%% Replace anchors with real docs URLs if needed
%% =========================
click CL_SAFE "#chat-layer-security" "Chat Layer safety checks"
click HL_EXEC "#harness-layer-agent-execution" "Harness Layer execution pipeline"
click PL_EVIDENCE "#policy-layer-evidence-gate" "Evidence gate details"
click PL_RISK "#policy-layer-clinical-risk-gate" "Clinical risk handling"
click DL_GRAPH "#data-layer-graphrag" "GraphRAG data model"
click EX_OBS "#observability-and-audit" "Exception observability"
```

## Review checklist

- Chat Layer validates identity, consent, prompt safety, and case intake boundaries before invoking agents.
- Harness Layer routes, plans, validates tools, executes agents, retries transient failures, and collects traces.
- Policy Layer blocks unsafe recommendations through evidence, clinical risk, fairness, privacy, and explainability gates.
- Data Layer enforces tenant checks, PHI/PII classification, GraphRAG retrieval, semantic search, and immutable audit logging.
