package com.berdachuk.medexpertmatch.graph.service.impl;

import com.berdachuk.medexpertmatch.graph.service.GraphService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphQueryServiceTest {

    @Mock
    private GraphService graphService;

    @InjectMocks
    private GraphQueryServiceImpl graphQueryService;

    @Test
    @DisplayName("direct relationship score returns 1.0 when TREATED relationship exists")
    void directRelationshipScoreTreated() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("c", 1)), List.of(Map.of("c", 0)));

        double score = graphQueryService.calculateDirectRelationshipScore("doc-1", "case-1", "sess-1");
        assertEquals(1.0, score);
    }

    @Test
    @DisplayName("direct relationship score returns 0.0 when no relationships exist")
    void directRelationshipScoreNone() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("c", 0)), List.of(Map.of("c", 0)));

        double score = graphQueryService.calculateDirectRelationshipScore("doc-1", "case-1", "sess-1");
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("condition expertise score returns 0.5 for empty ICD-10 codes")
    void conditionExpertiseEmptyCodes() {
        double score = graphQueryService.calculateConditionExpertiseScore("doc-1", List.of(), "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("condition expertise score returns 0.5 for null ICD-10 codes")
    void conditionExpertiseNullCodes() {
        double score = graphQueryService.calculateConditionExpertiseScore("doc-1", null, "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("condition expertise score returns 1.0 when all codes match")
    void conditionExpertiseAllMatch() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("c", 1)));

        double score = graphQueryService.calculateConditionExpertiseScore(
                "doc-1", List.of("I21", "I10"), "sess-1");
        assertEquals(1.0, score);
    }

    @Test
    @DisplayName("specialization match score returns 0.5 for null specialty")
    void specializationMatchNullSpecialty() {
        double score = graphQueryService.calculateSpecializationMatchScore("doc-1", null, "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("specialization match score returns 0.0 when no specialties found")
    void specializationMatchNoResults() {
        when(graphService.executeCypher(anyString(), anyMap())).thenReturn(List.of());

        double score = graphQueryService.calculateSpecializationMatchScore("doc-1", "Cardiology", "sess-1");
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("specialization match score returns 1.0 for exact match")
    void specializationMatchExact() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("name", "Cardiology")));

        double score = graphQueryService.calculateSpecializationMatchScore("doc-1", "Cardiology", "sess-1");
        assertEquals(1.0, score);
    }

    @Test
    @DisplayName("similar cases score returns 0.5 for empty ICD-10 codes")
    void similarCasesEmptyCodes() {
        double score = graphQueryService.calculateSimilarCasesScore("doc-1", List.of(), "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("similar cases score returns 0.5 for null ICD-10 codes")
    void similarCasesNullCodes() {
        double score = graphQueryService.calculateSimilarCasesScore("doc-1", null, "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("similar cases score returns 0.5 for one similar case")
    void similarCasesOne() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("c", 1)));

        double score = graphQueryService.calculateSimilarCasesScore(
                "doc-1", List.of("I21"), "sess-1");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("similar cases score returns 1.0 for many similar cases")
    void similarCasesMany() {
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("c", 10)));

        double score = graphQueryService.calculateSimilarCasesScore(
                "doc-1", List.of("I21"), "sess-1");
        assertEquals(1.0, score);
    }
}
