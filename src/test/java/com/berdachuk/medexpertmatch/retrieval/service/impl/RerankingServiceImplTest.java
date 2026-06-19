package com.berdachuk.medexpertmatch.retrieval.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerankingServiceImplTest {

    @Test
    @DisplayName("parseLineBasedIndices parses valid line-based indices")
    void parseLineBasedIndicesValid() throws Exception {
        String input = "3\n0\n5\n1\n2\n4\n";
        Method method = RerankingServiceImpl.class.getDeclaredMethod("parseLineBasedIndices", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(null, input);
        assertEquals(List.of(3, 0, 5, 1, 2, 4), result);
    }

    @Test
    @DisplayName("parseLineBasedIndices handles blank lines")
    void parseLineBasedIndicesWithBlankLines() throws Exception {
        String input = "3\n\n0\n\n5\n";
        Method method = RerankingServiceImpl.class.getDeclaredMethod("parseLineBasedIndices", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(null, input);
        assertEquals(List.of(3, 0, 5), result);
    }

    @Test
    @DisplayName("parseLineBasedIndices returns empty for null input")
    void parseLineBasedIndicesNull() throws Exception {
        Method method = RerankingServiceImpl.class.getDeclaredMethod("parseLineBasedIndices", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(null, (Object) null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseLineBasedIndices returns empty for blank input")
    void parseLineBasedIndicesBlank() throws Exception {
        Method method = RerankingServiceImpl.class.getDeclaredMethod("parseLineBasedIndices", String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(null, "   ");
        assertTrue(result.isEmpty());
    }
}
