package com.berdachuk.medexpertmatch.documents.rest;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchFilters;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DocumentSearchV2Controller}. Validates faceted-search shape
 * (data, facets, meta), date parsing tolerance, minScore filtering, and validation.
 */
@ExtendWith(MockitoExtension.class)
class DocumentSearchV2ControllerTest {

    @Mock
    private DocumentSearchApi documentSearchApi;

    @InjectMocks
    private DocumentSearchV2Controller controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        MethodValidationInterceptor interceptor = new MethodValidationInterceptor(validator);
        ProxyFactory proxyFactory = new ProxyFactory(controller);
        proxyFactory.addAdvice(interceptor);
        DocumentSearchV2Controller proxied = (DocumentSearchV2Controller) proxyFactory.getProxy();
        mockMvc = MockMvcBuilders.standaloneSetup(proxied)
                .setControllerAdvice(new ConstraintViolationExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("v2 search returns data, facets and meta envelope")
    void searchReturnsFacetedEnvelope() throws Exception {
        DocumentSearchResult r1 = new DocumentSearchResult(
                "c1", "d1", 0, "t", "D", "guideline", "pubmed", 0.9);
        DocumentSearchResult r2 = new DocumentSearchResult(
                "c2", "d2", 0, "t", "D", "review", "cochrane", 0.85);
        DocumentSearchResult r3 = new DocumentSearchResult(
                "c3", "d3", 0, "t", "D", null, null, 0.7);
        when(documentSearchApi.searchChunksFaceted(eq("q"), eq(10), any(DocumentSearchFilters.class)))
                .thenReturn(List.of(r1, r2, r3));

        mockMvc.perform(get("/api/v2/documents/search").param("query", "q"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.facets.categories.guideline").value(1))
                .andExpect(jsonPath("$.facets.categories.review").value(1))
                .andExpect(jsonPath("$.facets.categories.unknown").value(1))
                .andExpect(jsonPath("$.facets.sources.pubmed").value(1))
                .andExpect(jsonPath("$.facets.sources.cochrane").value(1))
                .andExpect(jsonPath("$.facets.sources.unknown").value(1))
                .andExpect(jsonPath("$.meta.version").value("2.0"))
                .andExpect(jsonPath("$.meta.totalResults").value(3));
    }

    @Test
    @DisplayName("v2 search forwards date and category filters to the service")
    void searchForwardsFilters() throws Exception {
        when(documentSearchApi.searchChunksFaceted(eq("q"), eq(5), any(DocumentSearchFilters.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v2/documents/search")
                        .param("query", "q")
                        .param("limit", "5")
                        .param("category", "guideline")
                        .param("source", "pubmed")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalResults").value(0));

        ArgumentCaptor<DocumentSearchFilters> filterCaptor = ArgumentCaptor.forClass(DocumentSearchFilters.class);
        verify(documentSearchApi).searchChunksFaceted(eq("q"), eq(5), filterCaptor.capture());
        DocumentSearchFilters filters = filterCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("guideline", filters.category());
        org.junit.jupiter.api.Assertions.assertEquals("pubmed", filters.source());
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2024, 1, 1), filters.fromDate());
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2024, 12, 31), filters.toDate());
    }

    @Test
    @DisplayName("v2 search silently ignores malformed date strings and uses null bounds")
    void searchToleratesMalformedDates() throws Exception {
        when(documentSearchApi.searchChunksFaceted(any(), anyInt(), any(DocumentSearchFilters.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v2/documents/search")
                        .param("query", "q")
                        .param("from", "not-a-date")
                        .param("to", "also-not-a-date"))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentSearchFilters> filterCaptor = ArgumentCaptor.forClass(DocumentSearchFilters.class);
        verify(documentSearchApi).searchChunksFaceted(any(), anyInt(), filterCaptor.capture());
        DocumentSearchFilters filters = filterCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertNull(filters.fromDate());
        org.junit.jupiter.api.Assertions.assertNull(filters.toDate());
    }

    @Test
    @DisplayName("v2 search filters out results below minScore and updates facets accordingly")
    void searchAppliesMinScore() throws Exception {
        DocumentSearchResult hi = new DocumentSearchResult(
                "c1", "d1", 0, "t", "D", "guideline", "pubmed", 0.9);
        DocumentSearchResult lo = new DocumentSearchResult(
                "c2", "d2", 0, "t", "D", "guideline", "pubmed", 0.3);
        when(documentSearchApi.searchChunksFaceted(any(), anyInt(), any(DocumentSearchFilters.class)))
                .thenReturn(List.of(hi, lo));

        mockMvc.perform(get("/api/v2/documents/search")
                        .param("query", "q")
                        .param("minScore", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.facets.categories.guideline").value(1))
                .andExpect(jsonPath("$.meta.totalResults").value(1));
    }

    @Test
    @DisplayName("v2 search rejects blank query")
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v2/documents/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("v2 search rejects limit=0")
    void searchRejectsZeroLimit() throws Exception {
        mockMvc.perform(get("/api/v2/documents/search")
                        .param("query", "q")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }
}
