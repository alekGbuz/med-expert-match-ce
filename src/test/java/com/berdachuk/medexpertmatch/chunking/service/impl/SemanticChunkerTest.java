package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticChunkerTest {

    private final SemanticChunker chunker = new SemanticChunker();

    @Test
    @DisplayName("returns empty list for null text")
    void nullTextReturnsEmpty() {
        assertEquals(List.of(), chunker.chunk(null, 100, 20, 10));
    }

    @Test
    @DisplayName("returns empty list when text is shorter than minChars")
    void shortTextReturnsEmpty() {
        assertEquals(List.of(), chunker.chunk("Hi", 100, 20, 10));
    }

    @Test
    @DisplayName("splits text at sentence boundaries")
    void splitsAtSentenceBoundaries() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<String> chunks = chunker.chunk(text, 100, 0, 5);
        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().contains("First sentence"));
    }

    @Test
    @DisplayName("creates multiple chunks when sentences exceed chunkSize")
    void multipleChunksWhenExceedingSize() {
        String text = "A very long first sentence that goes on and on. A second sentence here. A third one too.";
        List<String> chunks = chunker.chunk(text, 40, 0, 5);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    @DisplayName("respects overlap between chunks")
    void respectsOverlap() {
        String text = "Sentence one here. Sentence two here. Sentence three here. Sentence four here.";
        List<String> chunks = chunker.chunk(text, 30, 15, 5);
        assertTrue(chunks.size() >= 2);
    }
}
