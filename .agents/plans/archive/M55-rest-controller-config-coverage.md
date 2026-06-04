# M55: REST Controller and Configuration Test Coverage

**Goal**: Improve test coverage for low-coverage REST controllers and configuration classes.

**Context**:
- M54 boosted system health/monitoring/shutdown coverage.
- 1161 total tests pass with zero failures.
- Weakest REST/config areas: `documents.rest` (7%), `embedding.config` (7%), `doctor.rest` (23%).
- Overall instruction coverage: ~70% (unit + IT).

## Steps

### 1. Documents REST Controller Tests

**Current**: `documents.rest` at 7% (150 of 162 instructions missed). `DocumentSearchController` and `DocumentSearchV2Controller` are essentially untested.

**Add**:
- `DocumentSearchControllerTest` — unit test for v1 search endpoint:
  - Successful search with query and topK parameters
  - Empty result set
  - Invalid parameters (negative topK, blank query)
  - Service exception handling
  - Mock `DocumentSearchService` to avoid DB calls
- `DocumentSearchV2ControllerTest` — unit test for v2 endpoint:
  - Versioned endpoint routing
  - Backward-compatible request/response shape
  - Same error and edge cases as v1

### 2. Embedding Configuration Tests

**Current**: `embedding.config` at 7% (219 of 236 instructions missed). `EmbeddingEndpointPoolConfig` and `MultiEndpointEmbeddingProperties` are auto-config classes with no test coverage.

**Add**:
- `EmbeddingEndpointPoolConfigTest` — unit test:
  - Bean wiring for `EmbeddingEndpointPool`
  - Conditional enabling via properties
  - Worker thread configuration
  - Health-indicator bean creation
- `MultiEndpointEmbeddingPropertiesTest` — unit test:
  - Default values
  - Property binding from `@ConfigurationProperties`
  - Endpoint list parsing
  - Skip duration validation

### 3. Doctor REST Controller Tests

**Current**: `doctor.rest` at 23% (66 of 86 instructions missed). `DoctorRestController` and `MedicalSpecialtyRestController` are lightly tested.

**Add**:
- `DoctorRestControllerTest` — unit test:
  - List/search doctors with filters
  - Get by ID (found / not found)
  - Create/update doctor validation
  - Pagination
  - Mock `DoctorRepository` and `DoctorService`
- `MedicalSpecialtyRestControllerTest` — unit test:
  - List specialties
  - Get specialty by ID
  - Not-found handling
  - Mock `MedicalSpecialtyRepository`

### 4. Coverage baseline and reporting

- Run JaCoCo coverage report
- Document per-package coverage improvements
- Update CHANGELOG with updated test counts and coverage figures

## Success Criteria

- [ ] `documents.rest` coverage ≥ 60% (from 7%)
- [ ] `embedding.config` coverage ≥ 60% (from 7%)
- [ ] `doctor.rest` coverage ≥ 60% (from 23%)
- [ ] All new tests pass with zero failures
- [ ] `mvn verify` passes with zero failures
- [ ] CHANGELOG updated

## Estimated Test Count

- 10-15 documents REST tests
- 8-12 embedding config tests
- 10-15 doctor REST tests
- **Total: ~30-45 new tests** → overall ≥ 1200 tests
