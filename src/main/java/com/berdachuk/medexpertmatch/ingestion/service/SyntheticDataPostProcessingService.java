package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;

/**
 * Post-processing service for generated synthetic data.
 */
public interface SyntheticDataPostProcessingService {

    /**
     * Clears generated synthetic data and graph objects.
     */
    void clearTestData();

    /**
     * Generates descriptions for medical cases without descriptions.
     *
     * @param progress Optional progress tracker
     */
    void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress);

    /**
     * Commits a batch of generated descriptions.
     *
     * @param batch Description updates to commit
     */
    void commitDescriptionBatch(List<CaseDescriptionUpdate> batch);

    /**
     * Generates missing embeddings.
     */
    void generateEmbeddings();

    /**
     * Generates missing embeddings with progress tracking.
     *
     * @param progress Optional progress tracker
     */
    void generateEmbeddings(SyntheticDataGenerationProgress progress);

    /**
     * Rebuilds the graph representation from relational data.
     */
    void buildGraph();

    /**
     * M73: walks the SQL doctor table and ensures every
     * {@code (d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)}
     * edge implied by {@code d.specialties} exists in the graph.
     * Idempotent; safe to call on a healthy graph.
     *
     * @return summary of what was processed; never {@code null}
     */
    ReconcileReport reconcileSpecialtyGraph();

    /**
     * Summary of a {@link #reconcileSpecialtyGraph()} run.
     *
     * @param processed         number of (doctor, specialty) pairs that were
     *                          passed to the graph builder (either newly
     *                          created or no-op due to MERGE idempotency)
     * @param doctorsProcessed  number of doctors scanned in this run
     * @param doctors           distinct doctor IDs that were touched
     * @param specialties       distinct specialty names that were touched
     */
    record ReconcileReport(int processed,
                           int doctorsProcessed,
                           List<String> doctors,
                           List<String> specialties) {
    }
}
