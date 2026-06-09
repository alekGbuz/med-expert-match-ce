package com.berdachuk.medexpertmatch.ingestion.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable shared state for synthetic data catalogs and derived lists.
 */
@Getter
@Setter
@Component
public class SyntheticDataCatalogState {

    private final Map<String, List<String>> cachedIcd10Codes = new ConcurrentHashMap<>();
    private final Map<String, List<String>> cachedSpecialties = new ConcurrentHashMap<>();
    private final Map<String, List<String>> cachedProcedures = new ConcurrentHashMap<>();
    private final Map<String, List<String>> specialtyProceduresMap = new HashMap<>();
    private final Map<String, SyntheticDataGenerationService.DataSizeConfig> dataSizeConfigs = new LinkedHashMap<>();

    private List<String> extendedProcedures = new ArrayList<>();
    private List<String> extendedIcd10Codes = new ArrayList<>();
    private List<String> loadedMedicalSpecialties = new ArrayList<>();
    private List<String> loadedIcd10Codes = new ArrayList<>();
    private List<String> loadedProcedures = new ArrayList<>();
    private List<String> loadedFacilityTypes = new ArrayList<>();
    private List<String> loadedComplexityLevels = new ArrayList<>();
    private List<String> loadedOutcomes = new ArrayList<>();
    private List<String> loadedAvailabilityStatuses = new ArrayList<>();
    private List<String> loadedSeverities = new ArrayList<>();
    private List<String> loadedSymptoms = new ArrayList<>();
    private List<String> loadedEncounterClasses = new ArrayList<>();
    private List<String> loadedEncounterTypes = new ArrayList<>();
    private List<String> loadedComplications = new ArrayList<>();
    private List<String> loadedFacilityCapabilities = new ArrayList<>();

    private Map<String, String> icd10CodeDisplays = new HashMap<>();
    private Map<String, String> encounterClassDisplays = new HashMap<>();
    private List<String> extendedMedicalSpecialties = new ArrayList<>();

    /**
     * M73: number of doctors per major specialty. Populated by
     * {@code SyntheticDataPostProcessingService.reconcileSpecialtyGraph()}
     * and exposed via the synthetic-data state endpoint so operators
     * can see whether the M73 minimum ({@code MIN_DOCTORS_PER_MAJOR_SPECIALTY = 3})
     * is satisfied. Read-only after the catalog is loaded; cleared on
     * {@code clearTestData()}.
     */
    private Map<String, Integer> primarySpecialtyCoverage = new LinkedHashMap<>();
}
