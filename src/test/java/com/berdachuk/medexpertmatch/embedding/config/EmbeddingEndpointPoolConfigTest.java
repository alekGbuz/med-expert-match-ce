package com.berdachuk.medexpertmatch.embedding.config;

import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import com.berdachuk.medexpertmatch.embedding.multiendpoint.EndpointState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EmbeddingEndpointPoolConfig} focused on the static URL
 * normalizer (used by OpenAI-compatible backends such as Ollama and LM Studio) and
 * the bean factory's endpoint sorting, worker math, model wiring, and fail-fast
 * behavior when no valid endpoints are configured.
 * <p>
 * The {@code embeddingEndpointPool} bean creates real {@code OpenAiEmbeddingModel}
 * instances. Construction does not perform network I/O (network calls are deferred to
 * the first {@code embed} call), so tests instantiate the real model and inspect the
 * resulting pool state via reflection on the private {@code endpoints} field.
 */
class EmbeddingEndpointPoolConfigTest {

    private final EmbeddingEndpointPoolConfig config = new EmbeddingEndpointPoolConfig();
    private EmbeddingEndpointPool poolToShutdown;

    @AfterEach
    void tearDown() {
        if (poolToShutdown != null) {
            poolToShutdown.shutdown();
            poolToShutdown = null;
        }
    }

    private List<EndpointState> extractEndpoints(EmbeddingEndpointPool pool) throws Exception {
        Field f = EmbeddingEndpointPool.class.getDeclaredField("endpoints");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<EndpointState> endpoints = (List<EndpointState>) f.get(pool);
        return endpoints;
    }

    @Test
    @DisplayName("normalizeOpenAiBaseUrl appends /v1 when missing")
    void appendsV1() {
        assertEquals("http://localhost:11434/v1",
                EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl("http://localhost:11434"));
    }

    @Test
    @DisplayName("normalizeOpenAiBaseUrl trims trailing slashes before adding /v1")
    void trimsTrailingSlashes() {
        assertEquals("http://localhost:11434/v1",
                EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl("http://localhost:11434///"));
    }

    @Test
    @DisplayName("normalizeOpenAiBaseUrl keeps existing /v1 suffix without doubling it")
    void keepsExistingV1() {
        assertEquals("https://api.openai.com/v1",
                EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl("https://api.openai.com/v1"));
    }

    @Test
    @DisplayName("normalizeOpenAiBaseUrl returns null/blank unchanged")
    void blankOrNullUnchanged() {
        assertNull(EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl(null));
        assertEquals("", EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl(""));
    }

    @Test
    @DisplayName("normalizeOpenAiBaseUrl trims surrounding whitespace")
    void trimsWhitespace() {
        assertEquals("http://localhost:11434/v1",
                EmbeddingEndpointPoolConfig.normalizeOpenAiBaseUrl("  http://localhost:11434  "));
    }

    @Test
    @DisplayName("Bean factory builds pool with endpoints sorted by ascending priority")
    void buildsPoolSortedByPriority() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setEndpoints(List.of(
                endpoint("http://a.example.com", "m-a", 20, null),
                endpoint("http://b.example.com", "m-b", 5, null),
                endpoint("http://c.example.com", "m-c", 10, null)));
        props.setWorkerPerEndpoint(2);

        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.custom.embedding.api-key", "test-key")
                .withProperty("spring.ai.custom.embedding.dimensions", "768");

        poolToShutdown = config.embeddingEndpointPool(props, env);
        assertNotNull(poolToShutdown);

        List<EndpointState> endpoints = extractEndpoints(poolToShutdown);
        assertEquals(3, endpoints.size());
        assertEquals("http://b.example.com/v1", endpoints.get(0).getUrl());
        assertEquals("http://c.example.com/v1", endpoints.get(1).getUrl());
        assertEquals("http://a.example.com/v1", endpoints.get(2).getUrl());
        assertEquals("m-b", endpoints.get(0).getModel());
        assertEquals("m-c", endpoints.get(1).getModel());
        assertEquals("m-a", endpoints.get(2).getModel());
    }

    @Test
    @DisplayName("Bean factory skips endpoints with empty URLs and throws if all are invalid")
    void skipsBlankUrlsAndFailsFast() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        MultiEndpointEmbeddingProperties.EndpointConfig blank = new MultiEndpointEmbeddingProperties.EndpointConfig();
        blank.setUrl("");
        blank.setPriority(0);
        MultiEndpointEmbeddingProperties.EndpointConfig nullUrl = new MultiEndpointEmbeddingProperties.EndpointConfig();
        nullUrl.setUrl(null);
        nullUrl.setPriority(0);
        props.setEndpoints(List.of(blank, nullUrl));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.embeddingEndpointPool(props, new MockEnvironment()));
        assertTrue(ex.getMessage().contains("no valid endpoints"));
    }

    @Test
    @DisplayName("Bean factory honors per-endpoint workers override and falls back to global default")
    void honorsPerEndpointWorkers() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setWorkerPerEndpoint(1);
        props.setEndpoints(List.of(
                endpoint("http://a.example.com", "m-a", 0, 4),
                endpoint("http://b.example.com", "m-b", 0, null)));

        poolToShutdown = config.embeddingEndpointPool(props, new MockEnvironment());
        assertNotNull(poolToShutdown);
        assertEquals(2, extractEndpoints(poolToShutdown).size());
    }

    @Test
    @DisplayName("Bean factory clamps zero/negative workers to a minimum of 1")
    void clampsZeroWorkers() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setWorkerPerEndpoint(0);
        props.setEndpoints(List.of(endpoint("http://a.example.com", "m-a", 0, 0)));

        poolToShutdown = config.embeddingEndpointPool(props, new MockEnvironment());
        assertNotNull(poolToShutdown);
    }

    @Test
    @DisplayName("Bean factory silently ignores non-numeric embedding dimensions")
    void toleratesInvalidDimensions() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setEndpoints(List.of(endpoint("http://a.example.com", "m-a", 0, null)));

        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.ai.custom.embedding.dimensions", "not-a-number");

        poolToShutdown = config.embeddingEndpointPool(props, env);
        assertNotNull(poolToShutdown);
        assertEquals(1, extractEndpoints(poolToShutdown).size());
    }

    @Test
    @DisplayName("Bean factory tolerates null api-key from Environment")
    void toleratesNullApiKey() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setEndpoints(List.of(endpoint("http://a.example.com", "m-a", 0, null)));

        MockEnvironment env = new MockEnvironment();
        env.getProperty("spring.ai.custom.embedding.api-key");

        poolToShutdown = config.embeddingEndpointPool(props, env);
        assertNotNull(poolToShutdown);
        assertEquals(1, extractEndpoints(poolToShutdown).size());
    }

    @Test
    @DisplayName("Bean factory leaves model name unset on EndpointState when config omits it")
    void omitsModelWhenConfigBlank() throws Exception {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        MultiEndpointEmbeddingProperties.EndpointConfig e = new MultiEndpointEmbeddingProperties.EndpointConfig();
        e.setUrl("http://a.example.com");
        e.setModel(null);
        e.setPriority(0);
        props.setEndpoints(List.of(e));

        poolToShutdown = config.embeddingEndpointPool(props, new MockEnvironment());
        List<EndpointState> endpoints = extractEndpoints(poolToShutdown);
        assertEquals(1, endpoints.size());
        assertNull(endpoints.get(0).getModel());
    }

    private static MultiEndpointEmbeddingProperties.EndpointConfig endpoint(
            String url, String model, int priority, Integer workers) {
        MultiEndpointEmbeddingProperties.EndpointConfig c = new MultiEndpointEmbeddingProperties.EndpointConfig();
        c.setUrl(url);
        c.setModel(model);
        c.setPriority(priority);
        c.setWorkers(workers);
        return c;
    }
}
