# M53: Ingestion Module Test Coverage and Remaining Coverage Gaps

**Goal**: Improve test coverage in the ingestion module (currently ~24%) and address remaining low-coverage modules to bring overall instruction coverage above 75%.

**Context**:
- M51 added health indicators, graceful shutdown, and coverage in evidence/documents/retrieval modules.
- M52 introduced WireMock for mocking external HTTP services in ITs.
- Ingestion module (FHIR adapters, synthetic data generation) has ~24% coverage — the weakest module.
- 517 tests pass with zero failures.
- 533 main Java source files, 214 test source files.

## Steps

### 1. Ingestion FHIR Adapter Unit Tests

**Current**: `FhirAdapterIT` has 12 tests covering high-level FHIR bundle generation. Missing coverage for individual FHIR adapter components.

**Add**:
- `FhirR5DoctorAdapterTest` — unit test for doctor-to-FHIR mapping:
  - Single doctor conversion with all fields populated
  - Doctor with empty specializations list
  - Doctor with null availability status
- `FhirR5CaseAdapterTest` — unit test for medical case-to-FHIR mapping:
  - Full case conversion with ICD-10 and SNOMED codes
  - Case with minimal fields
  - Case with null urgency/type fallback handling
- Use WireMock pattern from M52 for any external FHIR validation endpoints.

### 2. Synthetic Data Generator Edge Case Tests

**Current**: `SyntheticDataGeneratorIT` has 32 tests covering main generation paths.

**Add**:
- `SyntheticDataEdgeCasesIT` — extension tests:
  - Empty catalog handling
  - Size=0 parameter validation
  - Duplicate doctor/case ID prevention
  - Null safety in all generator services
  - Large batch generation memory threshold

### 3. Ingestion Post-processing Tests

**Add**:
- `SyntheticDataPostProcessingIT` — tests for data clearing and graph rebuilding:
  - Clear graph with existing data
  - Clear graph with empty graph
  - Sequential clear-generate-clear cycle

### 4. Coverage baseline and reporting

- Run JaCoCo coverage report
- Document current per-module coverage
- Update CHANGELOG with updated test counts and coverage figures

## Success Criteria

- [ ] Ingestion module coverage ≥ 40% (from ~24%)
- [ ] All new tests pass with zero failures
- [ ] Overall test count ≥ 525
- [ ] `mvn verify` passes with zero failures
- [ ] Coverage report generated and documented
- [ ] CHANGELOG updated
