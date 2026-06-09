package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.ingestion.service.DoctorGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for generating doctors.
 */
@Slf4j
@Service
public class DoctorGeneratorServiceImpl implements DoctorGeneratorService {

    private static final int MAX_DOCTORS_PER_BATCH = 100000;
    private static final int MIN_SPECIALTIES_PER_DOCTOR = 1;
    private static final int MAX_SPECIALTIES_PER_DOCTOR = 3;
    private static final double TELEHEALTH_PROBABILITY = 0.6;
    private static final int MIN_FACILITIES_PER_DOCTOR = 0;
    private static final int MAX_FACILITIES_PER_DOCTOR = 3;

    /**
     * Major medical specialties that the M73 plan guarantees at least
     * {@link #MIN_DOCTORS_PER_MAJOR_SPECIALTY} doctors for. The list is
     * ordered by clinical priority so that the round-robin primary
     * assignment covers the most important specialties first even in
     * tiny synthetic populations.
     */
    public static final List<String> MAJOR_SPECIALTIES = List.of(
            "Cardiology",
            "Oncology",
            "Urology",
            "Hematology-Oncology",
            "Medical Oncology",
            "Nephrology",
            "Neurology",
            "Pediatrics",
            "Family Medicine",
            "Internal Medicine",
            "Surgery"
    );

    /**
     * M73 acceptance criterion: every major specialty must have at
     * least this many doctors in the generated pool. Guarantees a
     * meaningful match when a case queries for a specific specialty.
     */
    public static final int MIN_DOCTORS_PER_MAJOR_SPECIALTY = 3;

    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final Faker faker = new Faker();
    private final Random random = new Random();
    private final Counter doctorsGeneratedCounter;

    public DoctorGeneratorServiceImpl(
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            MeterRegistry meterRegistry) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.doctorsGeneratedCounter = Counter.builder("synthetic.data.doctors.generated")
                .description("Total number of doctors generated")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public void generateDoctors(int count, SyntheticDataGenerationProgress progress,
                                  List<String> medicalSpecialties, List<String> availabilityStatuses) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }
        if (count > MAX_DOCTORS_PER_BATCH) {
            throw new IllegalArgumentException(
                    String.format("Count exceeds maximum: %d (max: %d)", count, MAX_DOCTORS_PER_BATCH));
        }

        List<String> loadedMedicalSpecialties = medicalSpecialties != null && !medicalSpecialties.isEmpty()
                ? new ArrayList<>(medicalSpecialties) : List.of("General Medicine");
        List<String> loadedAvailabilityStatuses = availabilityStatuses != null && !availabilityStatuses.isEmpty()
                ? availabilityStatuses : List.of("AVAILABLE");

        log.info("Generating {} doctors", count);

        // Load actual facility IDs from database
        List<String> availableFacilityIds = facilityRepository.findAllIds(0);
        if (availableFacilityIds.isEmpty()) {
            log.warn("No facilities found in database. Doctors will be created without facility affiliations.");
        }

        Set<String> generatedEmails = new HashSet<>();
        List<Doctor> doctors = new ArrayList<>();
        // Track which facilities have been assigned to at least one doctor
        Set<String> assignedFacilityIds = new HashSet<>();
        // M73: primary-specialty round-robin is now done by
        // assignSpecialties() below; the old shuffled queue was
        // removed in favour of the deterministic round-robin that
        // guarantees every major specialty has at least
        // MIN_DOCTORS_PER_MAJOR_SPECIALTY doctors.
        for (int i = 0; i < count; i++) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during doctor generation at {}/{}", i, count);
                return;
            }

            if (progress != null && count > 0 && (i + 1) % Math.max(1, count / 10) == 0) {
                int doctorProgress = 20 + ((i + 1) * 15 / count);
                progress.updateProgress(doctorProgress, "Doctors",
                        String.format("Generating doctors: %d/%d", i + 1, count));
            }

            String doctorId = IdGenerator.generateDoctorId();
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String name = "Dr. " + firstName + " " + lastName;

            String emailSuffix = doctorId.length() >= 8 ? doctorId.substring(0, 8) : doctorId;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + emailSuffix + "@medical.example.com";

            int emailCounter = 1;
            String originalEmail = email;
            while (generatedEmails.contains(email)) {
                email = originalEmail.replace("@medical.example.com",
                        "." + emailCounter + "@medical.example.com");
                emailCounter++;
            }
            generatedEmails.add(email);

            // M73: use the round-robin primary-specialty helper so every
            // major specialty gets at least MIN_DOCTORS_PER_MAJOR_SPECIALTY
            // doctors. The first element of the returned list is the
            // primary; the remaining 0-2 are random secondaries.
            List<String> specialties = assignSpecialties(i, count, loadedMedicalSpecialties, random);

            List<String> certifications = new ArrayList<>();
            if (random.nextBoolean()) {
                certifications.add("Board Certified");
            }
            if (random.nextBoolean()) {
                certifications.add("Fellowship Trained");
            }

            Set<String> facilityIdsSet = new LinkedHashSet<>();
            int facilityCount = random.nextInt(MAX_FACILITIES_PER_DOCTOR - MIN_FACILITIES_PER_DOCTOR + 1) + MIN_FACILITIES_PER_DOCTOR;

            // Only assign facilities if they exist in the database
            if (!availableFacilityIds.isEmpty() && facilityCount > 0) {
                // Limit facility count to available facilities
                int maxFacilities = Math.min(facilityCount, availableFacilityIds.size());
                List<String> shuffledFacilities = new ArrayList<>(availableFacilityIds);
                Collections.shuffle(shuffledFacilities, random);

                for (int j = 0; j < maxFacilities; j++) {
                    facilityIdsSet.add(shuffledFacilities.get(j));
                }
            }

            // Ensure all facilities have at least one affiliated doctor
            // If this is one of the last doctors and there are still unassigned facilities, assign one
            if (!availableFacilityIds.isEmpty()) {
                List<String> remainingUnassigned = availableFacilityIds.stream()
                        .filter(fid -> !assignedFacilityIds.contains(fid))
                        .toList();
                if (!remainingUnassigned.isEmpty()) {
                    // Assign one unassigned facility to this doctor
                    String unassignedFacility = remainingUnassigned.get(random.nextInt(remainingUnassigned.size()));
                    facilityIdsSet.add(unassignedFacility);
                }
            }
            assignedFacilityIds.addAll(facilityIdsSet);

            List<String> facilityIds = new ArrayList<>(facilityIdsSet);

            boolean telehealthEnabled = random.nextDouble() < TELEHEALTH_PROBABILITY;
            String availabilityStatus = loadedAvailabilityStatuses.get(random.nextInt(loadedAvailabilityStatuses.size()));

            Doctor doctor = new Doctor(
                    doctorId,
                    name,
                    email,
                    specialties,
                    certifications,
                    facilityIds,
                    telehealthEnabled,
                    availabilityStatus
            );

            doctors.add(doctor);
        }

        batchProcess(
                doctors,
                Doctor::id,
                (ids) -> doctorRepository.findByIds(ids).stream()
                        .collect(Collectors.toMap(Doctor::id, Function.identity())),
                doctorRepository::insertBatch,
                doctorRepository::updateBatch,
                "doctors"
        );

        doctorsGeneratedCounter.increment(count);
        log.info("Generated {} doctors", count);
    }

    // -----------------------------------------------------------------
    // M73 — primary specialty assignment (pure helpers, unit-tested).
    // The pre-M73 code picked specialties randomly from the full
    // catalog, which produced a sparse distribution (1-2 doctors per
    // critical specialty). M73 guarantees ≥ MIN_DOCTORS_PER_MAJOR_SPECIALTY
    // doctors per major specialty with a single primary each.
    // -----------------------------------------------------------------

    /**
     * Returns the primary specialty for the doctor at the given
     * {@code doctorIndex} (0-based). Round-robins through
     * {@link #MAJOR_SPECIALTIES} so a small population still covers
     * the most important specialties first. If a major specialty is
     * not in the loaded {@code allSpecialties} catalog, it is
     * silently skipped.
     */
    public List<String> pickPrimarySpecialty(int doctorIndex, List<String> allSpecialties, Random random) {
        if (allSpecialties == null || allSpecialties.isEmpty() || MAJOR_SPECIALTIES.isEmpty()) {
            return List.of();
        }
        // Build the list of majors that are actually in the catalog
        // (so we don't try to assign a specialty the operator
        // removed from the CSV).
        List<String> availableMajors = new ArrayList<>();
        for (String major : MAJOR_SPECIALTIES) {
            if (allSpecialties.contains(major)) {
                availableMajors.add(major);
            }
        }
        if (availableMajors.isEmpty()) {
            // No major in the catalog — fall back to the first
            // available specialty so the generator still produces
            // a valid (if not major) primary.
            return List.of(allSpecialties.get(0));
        }
        // Round-robin so the first N doctors get the first N
        // available majors, and subsequent doctors cycle through
        // them again. With ≥ MIN_DOCTORS_PER_MAJOR_SPECIALTY * 11
        // doctors, every major gets at least 3 primaries.
        return List.of(availableMajors.get(doctorIndex % availableMajors.size()));
    }

    /**
     * Returns the full list of specialties assigned to a single
     * doctor. The first element is always the primary specialty
     * (from {@link #pickPrimarySpecialty(int, List, Random)}); the
     * remaining 0-2 are secondary, picked at random from the rest of
     * the catalog so each doctor still has a believable profile.
     */
    public List<String> assignSpecialties(int doctorIndex, int totalDoctors,
                                          List<String> allSpecialties, Random random) {
        if (allSpecialties == null || allSpecialties.isEmpty() || totalDoctors <= 0) {
            return List.of();
        }
        List<String> primary = pickPrimarySpecialty(doctorIndex, allSpecialties, random);
        if (primary.isEmpty()) {
            return List.of();
        }
        int specialtyCount = random.nextInt(MAX_SPECIALTIES_PER_DOCTOR - MIN_SPECIALTIES_PER_DOCTOR + 1)
                + MIN_SPECIALTIES_PER_DOCTOR;
        java.util.LinkedHashSet<String> picked = new java.util.LinkedHashSet<>(primary);
        java.util.List<String> remaining = new java.util.ArrayList<>(allSpecialties);
        remaining.removeAll(picked);
        while (picked.size() < specialtyCount && !remaining.isEmpty()) {
            String s = remaining.get(random.nextInt(remaining.size()));
            picked.add(s);
            remaining.remove(s);
        }
        return new java.util.ArrayList<>(picked);
    }

    private <T> void batchProcess(
            List<T> items,
            Function<T, String> getId,
            Function<List<String>, Map<String, T>> getExistingItems,
            java.util.function.Consumer<List<T>> insertBatch,
            java.util.function.Consumer<List<T>> updateBatch,
            String entityName) {

        if (items.isEmpty()) {
            log.debug("No {} to process", entityName);
            return;
        }

        List<String> ids = items.stream().map(getId).toList();
        Map<String, T> existingItems = getExistingItems.apply(ids);
        Set<String> existingIds = existingItems.keySet();

        List<T> toInsert = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();

        for (T item : items) {
            String id = getId.apply(item);
            if (existingIds.contains(id)) {
                toUpdate.add(item);
            } else {
                toInsert.add(item);
            }
        }

        if (!toInsert.isEmpty()) {
            insertBatch.accept(toInsert);
            log.debug("Inserted {} new {}", toInsert.size(), entityName);
        }

        if (!toUpdate.isEmpty()) {
            updateBatch.accept(toUpdate);
            log.debug("Updated {} existing {}", toUpdate.size(), entityName);
        }
    }
}
