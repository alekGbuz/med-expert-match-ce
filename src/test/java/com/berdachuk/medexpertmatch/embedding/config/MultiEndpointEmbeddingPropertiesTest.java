package com.berdachuk.medexpertmatch.embedding.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.berdachuk.medexpertmatch.embedding.config.MultiEndpointEmbeddingProperties.EndpointConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MultiEndpointEmbeddingProperties}: bean defaults, property
 * binding, and bean validation ({@code @NotBlank} URL, {@code @Min} ints).
 */
class MultiEndpointEmbeddingPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("Defaults: empty endpoints, skipDurationMin=10, workerPerEndpoint=1, apiBatchSize=50")
    void appliesDefaults() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        assertNotNull(props.getEndpoints());
        assertTrue(props.getEndpoints().isEmpty());
        assertEquals(10, props.getSkipDurationMin());
        assertEquals(1, props.getWorkerPerEndpoint());
        assertEquals(50, props.getApiBatchSize());
    }

    @Test
    @DisplayName("EndpointConfig defaults: url blank, model null, priority=0, workers null")
    void endpointConfigDefaults() {
        EndpointConfig c = new EndpointConfig();
        assertNull(c.getUrl());
        assertNull(c.getModel());
        assertEquals(0, c.getPriority());
        assertNull(c.getWorkers());
    }

    @Test
    @DisplayName("Setters and getters round-trip values")
    void settersRoundTrip() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setSkipDurationMin(30);
        props.setWorkerPerEndpoint(4);
        props.setApiBatchSize(100);

        EndpointConfig e = new EndpointConfig();
        e.setUrl("http://localhost:11434");
        e.setModel("nomic-embed-text");
        e.setPriority(5);
        e.setWorkers(2);
        props.setEndpoints(List.of(e));

        assertEquals(30, props.getSkipDurationMin());
        assertEquals(4, props.getWorkerPerEndpoint());
        assertEquals(100, props.getApiBatchSize());
        assertEquals(1, props.getEndpoints().size());
        assertEquals("http://localhost:11434", props.getEndpoints().get(0).getUrl());
        assertEquals("nomic-embed-text", props.getEndpoints().get(0).getModel());
        assertEquals(5, props.getEndpoints().get(0).getPriority());
        assertEquals(2, props.getEndpoints().get(0).getWorkers());
    }

    @Test
    @DisplayName("Validation: blank URL is rejected on EndpointConfig")
    void rejectsBlankUrl() {
        EndpointConfig e = new EndpointConfig();
        e.setUrl("");
        Set<ConstraintViolation<EndpointConfig>> violations = validator.validate(e);
        assertEquals(1, violations.size());
        assertEquals("url", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    @DisplayName("Validation: skipDurationMin must be >= 1")
    void rejectsZeroSkipDuration() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setSkipDurationMin(0);
        Set<ConstraintViolation<MultiEndpointEmbeddingProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("skipDurationMin", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    @DisplayName("Validation: workerPerEndpoint must be >= 1")
    void rejectsZeroWorkersPerEndpoint() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setWorkerPerEndpoint(0);
        Set<ConstraintViolation<MultiEndpointEmbeddingProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("workerPerEndpoint", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    @DisplayName("Validation: apiBatchSize must be >= 1")
    void rejectsZeroApiBatchSize() {
        MultiEndpointEmbeddingProperties props = new MultiEndpointEmbeddingProperties();
        props.setApiBatchSize(0);
        Set<ConstraintViolation<MultiEndpointEmbeddingProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("apiBatchSize", violations.iterator().next().getPropertyPath().toString());
    }
}
