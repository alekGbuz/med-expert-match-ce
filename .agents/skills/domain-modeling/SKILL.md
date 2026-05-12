# Domain modeling (medical match)

## Description

Covers **clinical domain entities** and value objects owned by each module: cases, doctors, facilities, coding, retrieval results, and LLM job status types. Includes **HIPAA-oriented** rules for synthetic data and logs.

## When to use

- Adding or changing entities under `*/domain/` (e.g. `MedicalCase`, `Doctor`, `Facility`, `ConsultationMatch`, `ICD10Code`, `Procedure`).
- Mapping database columns to Java fields or designing new filters/DTOs.
- Any feature touching **patient-like** narrative data in tests or logs.

## Instructions

- **Ownership**: keep entity definitions in the module that owns the aggregate (`medicalcase` owns `MedicalCase`, `doctor` owns `Doctor`, `retrieval` owns match/score types, etc.).
- **Naming**: DB snake_case; API JSON camelCase; follow existing Lombok usage (`@Data`, records for DTOs where appropriate).
- **PHI**: never log real patient identifiers; tests use anonymized synthetic IDs only.
- **ICD-10**: validate format and hierarchy per project rules when exposing codes in APIs.
- **Ubiquitous language**: align REST and UI wording with domain terms already used in modules (e.g. urgency, specialty, match).

## Boundaries

- Do not put domain logic in REST controllers; use services.
- Do not duplicate the same aggregate root in two modules; use references by ID and orchestration where needed.
- Do not use real patient data in fixtures or examples.
