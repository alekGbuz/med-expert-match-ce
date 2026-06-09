package com.berdachuk.medexpertmatch.ingestion.service.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the major-specialty / primary-specialty assignment in
 * {@link DoctorGeneratorServiceImpl} (M73).
 *
 * <p>The pre-M73 code picked specialties randomly from the full list,
 * which produced a sparse distribution: critical specialties such as
 * Oncology or Urology got only 1-2 doctors, and a {@code findBySpecialty("Oncology")}
 * query returned a partial fall-back full pool instead of dedicated
 * specialists. M73 guarantees ≥ 3 doctors per major specialty with one
 * primary specialty each.
 *
 * <p>These tests target the pure function
 * {@link DoctorGeneratorServiceImpl#assignSpecialties(int, int, List, Random)}
 * so the algorithm is verifiable without spinning up the Spring
 * context or the database.
 */
class DoctorGeneratorServiceImplSpecialtyTest {

    private static final List<String> SAMPLE_SPECIALTIES = List.of(
            "Cardiology", "Oncology", "Urology", "Hematology-Oncology",
            "Medical Oncology", "Nephrology", "Neurology", "Pediatrics",
            "Family Medicine", "Internal Medicine", "Surgery",
            "Addiction Psychiatry", "Aerospace Medicine", "Allergy",
            "Anesthesiology", "Cardiac Surgery");

    // -----------------------------------------------------------------
    // Acceptance criterion: every major specialty has ≥ 3 doctors
    // when the generator runs at or above the M73 floor.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("M73: every major specialty has at least 3 doctors when total >= 3 * major count")
    void everyMajorSpecialtyHasMinimumDoctors() {
        DoctorGeneratorServiceImpl impl = newImpl();
        int doctorCount = DoctorGeneratorServiceImpl.MAJOR_SPECIALTIES.size() * 5; // generous
        Map<String, Integer> primaryCounts = primarySpecialtyCounts(impl, doctorCount);

        for (String major : DoctorGeneratorServiceImpl.MAJOR_SPECIALTIES) {
            int count = primaryCounts.getOrDefault(major, 0);
            assertTrue(count >= 3,
                    "Major specialty '" + major + "' has only " + count
                            + " primary doctors (expected >= 3)");
        }
    }

    // -----------------------------------------------------------------
    // Acceptance criterion: every generated doctor has at least one
    // primary specialty, and the primary is from MAJOR_SPECIALTIES.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("M73: every doctor has at least one primary specialty from MAJOR_SPECIALTIES")
    void everyDoctorHasPrimarySpecialty() {
        DoctorGeneratorServiceImpl impl = newImpl();
        Random random = new Random(42);
        int doctorCount = 30;
        List<List<String>> allSpecialties = generateSpecialties(impl, doctorCount, random);

        for (int i = 0; i < doctorCount; i++) {
            List<String> primary = impl.pickPrimarySpecialty(i, SAMPLE_SPECIALTIES, random);
            assertNotNull(primary, "doctor " + i + " must have a primary specialty");
            assertFalse(primary.isEmpty(), "doctor " + i + " primary must not be empty");
            assertTrue(DoctorGeneratorServiceImpl.MAJOR_SPECIALTIES.contains(primary.get(0)),
                    "doctor " + i + " primary '" + primary.get(0) + "' must be a major specialty");
            assertNotNull(allSpecialties.get(i));
            assertFalse(allSpecialties.get(i).isEmpty(),
                    "doctor " + i + " must have at least one specialty");
            assertEquals(primary.get(0), allSpecialties.get(i).get(0),
                    "doctor " + i + " first specialty should be the primary");
        }
    }

    // -----------------------------------------------------------------
    // Acceptance criterion: with a tiny population (e.g. 5 doctors)
    // the major specialties are still covered — round-robin distributes
    // the primaries evenly.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("M73: tiny population still covers the first N major specialties (round-robin)")
    void tinyPopulationStillCoversMajors() {
        DoctorGeneratorServiceImpl impl = newImpl();
        Random random = new Random(7);
        int n = Math.min(8, DoctorGeneratorServiceImpl.MAJOR_SPECIALTIES.size());
        Map<String, Integer> primaryCounts = primarySpecialtyCounts(impl, n, random);

        // First n doctors should each have a unique major specialty
        // (round-robin). This guarantees that small doctor counts
        // still cover the most important specialties first.
        assertEquals(n, primaryCounts.size(),
                "first " + n + " doctors should each have a distinct major specialty");
    }

    // -----------------------------------------------------------------
    // Acceptance criterion: count = 0 is a no-op.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("zero doctors produces an empty list of specialties")
    void zeroDoctorsProducesEmptyList() {
        DoctorGeneratorServiceImpl impl = newImpl();
        List<List<String>> allSpecialties = generateSpecialties(impl, 0, new Random());
        assertTrue(allSpecialties.isEmpty());
    }

    // -----------------------------------------------------------------
    // Acceptance criterion: a major specialty that is not in the
    // loaded catalog (the catalog may be a strict subset in tests)
    // is skipped gracefully — the generator must not crash.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("missing major specialty in catalog is skipped without crashing")
    void missingMajorSpecialtyInCatalogIsSkipped() {
        DoctorGeneratorServiceImpl impl = newImpl();
        Random random = new Random(1);
        // Catalog contains only one major — the rest are missing
        List<String> tinyCatalog = List.of("Cardiology", "Some Rare Specialty");
        Map<String, Integer> primaryCounts = primarySpecialtyCounts(impl, 20, random, tinyCatalog);

        // All primaries should still be in MAJOR_SPECIALTIES (we only have one)
        for (String primary : primaryCounts.keySet()) {
            assertTrue(DoctorGeneratorServiceImpl.MAJOR_SPECIALTIES.contains(primary)
                            || primary.equals("Cardiology"),
                    "primary '" + primary + "' must be a major or the only available specialty");
        }
    }

    private static DoctorGeneratorServiceImpl newImpl() {
        return new DoctorGeneratorServiceImpl(null, null, new SimpleMeterRegistry());
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static Map<String, Integer> primarySpecialtyCounts(
            DoctorGeneratorServiceImpl impl, int doctorCount) {
        return primarySpecialtyCounts(impl, doctorCount, new Random(0), SAMPLE_SPECIALTIES);
    }

    private static Map<String, Integer> primarySpecialtyCounts(
            DoctorGeneratorServiceImpl impl, int doctorCount, Random random) {
        return primarySpecialtyCounts(impl, doctorCount, random, SAMPLE_SPECIALTIES);
    }

    private static Map<String, Integer> primarySpecialtyCounts(
            DoctorGeneratorServiceImpl impl, int doctorCount, Random random,
            List<String> catalog) {
        List<List<String>> all = generateSpecialties(impl, doctorCount, random, catalog);
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (List<String> specs : all) {
            if (!specs.isEmpty()) {
                counts.merge(specs.get(0), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static List<List<String>> generateSpecialties(
            DoctorGeneratorServiceImpl impl, int doctorCount, Random random) {
        return generateSpecialties(impl, doctorCount, random, SAMPLE_SPECIALTIES);
    }

    private static List<List<String>> generateSpecialties(
            DoctorGeneratorServiceImpl impl, int doctorCount, Random random,
            List<String> catalog) {
        List<List<String>> all = new ArrayList<>();
        for (int i = 0; i < doctorCount; i++) {
            all.add(impl.assignSpecialties(i, doctorCount, catalog, random));
        }
        return all;
    }
}
