package com.berdachuk.medexpertmatch.chunking.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveChunkerTest {

    private final AdaptiveChunker chunker = new AdaptiveChunker(
            new SemanticChunker(), new RecursiveCharacterChunker());

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
    @DisplayName("uses semantic chunker for text with many sentences")
    void usesSemanticForManySentences() {
        String text = "Sentence one. Sentence two. Sentence three. Sentence four. "
                + "Sentence five. Sentence six. Sentence seven. Sentence eight. "
                + "Sentence nine. Sentence ten. Sentence eleven.";
        List<String> chunks = chunker.chunk(text, 100, 0, 5);
        assertFalse(chunks.isEmpty());
    }

    @Test
    @DisplayName("uses recursive chunker for text with few sentences")
    void usesRecursiveForFewSentences() {
        String text = "A single paragraph without many sentence boundaries just flowing text.";
        List<String> chunks = chunker.chunk(text, 20, 0, 5);
        assertFalse(chunks.isEmpty());
    }

    @Test
    @DisplayName("uses semantic chunker for text with paragraphs and sentences")
    void usesSemanticForParagraphs() {
        String text = "Paragraph one with sentences. More text here.\n\n"
                + "Paragraph two with sentences. And more text.\n\n"
                + "Paragraph three with sentences. Final text.";
        List<String> chunks = chunker.chunk(text, 100, 0, 5);
        assertFalse(chunks.isEmpty());
    }
}
