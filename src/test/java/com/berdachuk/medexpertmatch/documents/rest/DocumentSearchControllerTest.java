package com.berdachuk.medexpertmatch.documents.rest;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link DocumentSearchController}. Pure MockMvc test (no Spring context,
 * no Testcontainers) — focuses on controller contract: query forwarding, minScore
 * filtering, validation, health endpoint.
 */
@ExtendWith(MockitoExtension.class)
class DocumentSearchControllerTest {

    @Mock
    private DocumentSearchApi documentSearchApi;

    @InjectMocks
    private DocumentSearchController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        MethodValidationInterceptor interceptor = new MethodValidationInterceptor(validator);
        ProxyFactory proxyFactory = new ProxyFactory(controller);
        proxyFactory.addAdvice(interceptor);
        DocumentSearchController proxied = (DocumentSearchController) proxyFactory.getProxy();
        mockMvc = MockMvcBuilders.standaloneSetup(proxied)
                .setControllerAdvice(new ConstraintViolationExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("search returns 200 and JSON results for valid query and default limit")
    void searchReturnsResults() throws Exception {
        DocumentSearchResult r1 = new DocumentSearchResult(
                "c1", "d1", 0, "text A", "Doc 1", "guideline", "pubmed", 0.95);
        DocumentSearchResult r2 = new DocumentSearchResult(
                "c2", "2", 1, "text B", "Doc 2", "guideline", "pubmed", 0.80);
        when(documentSearchApi.searchChunks("cardiology", 10)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/documents/search").param("query", "cardiology"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].chunkId").value("c1"))
                .andExpect(jsonPath("$[0].similarity").value(0.95))
                .andExpect(jsonPath("$[1].chunkId").value("c2"));

        verify(documentSearchApi).searchChunks("cardiology", 10);
    }

    @Test
    @DisplayName("search honors explicit limit and forwards it to the service")
    void searchHonorsLimit() throws Exception {
        when(documentSearchApi.searchChunks(eq("x"), eq(5))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "x")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(documentSearchApi).searchChunks("x", 5);
    }

    @Test
    @DisplayName("search filters out results whose similarity is below minScore")
    void searchAppliesMinScore() throws Exception {
        DocumentSearchResult hi = new DocumentSearchResult(
                "c1", "d1", 0, "t", "D", "g", "s", 0.9);
        DocumentSearchResult lo = new DocumentSearchResult(
                "c2", "2", 1, "t", "D", "g", "s", 0.3);
        when(documentSearchApi.searchChunks("q", 10)).thenReturn(List.of(hi, lo));

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "q")
                        .param("minScore", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chunkId").value("c1"));
    }

    @Test
    @DisplayName("search with blank query is rejected by bean validation")
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search").param("query", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search with limit=0 is rejected (must be >= 1)")
    void searchRejectsZeroLimit() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "q")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search with limit above 100 is rejected")
    void searchRejectsOverlargeLimit() throws Exception {
        mockMvc.perform(get("/api/v1/documents/search")
                        .param("query", "q")
                        .param("limit", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("health endpoint reports UP when the service responds")
    void healthReportsUp() throws Exception {
        when(documentSearchApi.searchChunks("test", 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/documents/search/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("health endpoint reports DOWN with error message when the service throws")
    void healthReportsDownOnException() throws Exception {
        when(documentSearchApi.searchChunks("test", 1))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/documents/search/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.error", containsString("boom")));
    }
}
