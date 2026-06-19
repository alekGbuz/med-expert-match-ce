package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveCharacterChunkerTest {

    private final RecursiveCharacterChunker chunker = new RecursiveCharacterChunker();

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
    @DisplayName("chunks text at sentence boundaries")
    void chunksAtSentenceBoundaries() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<String> chunks = chunker.chunk(text, 30, 0, 5);
        assertFalse(chunks.isEmpty());
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 35, "chunk too long: " + chunk.length());
        }
    }

    @Test
    @DisplayName("respects overlap parameter")
    void respectsOverlap() {
        String text = "A long text that should be split into multiple chunks with overlap between them.";
        List<String> chunks = chunker.chunk(text, 20, 10, 5);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    @DisplayName("single chunk when text fits within chunkSize")
    void singleChunkWhenFits() {
        String text = "Short text.";
        List<String> chunks = chunker.chunk(text, 100, 0, 5);
        assertEquals(1, chunks.size());
        assertEquals("Short text.", chunks.getFirst());
    }
}
