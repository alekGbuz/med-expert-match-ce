package com.berdachuk.medexpertmatch.medicalcase.service;

import java.util.Locale;

/**
 * Strips chain-of-thought leakage from LLM-generated case abstracts used for embeddings.
 */
public final class EmbeddingDescriptionSanitizer {

    private static final String[] CHAIN_OF_THOUGHT_MARKERS = {
            "mental sandbox",
            "confidence score",
            "constraint checklist",
            "final plan:",
            "key learnings",
            "attempt 1",
            "attempt 2",
            "attempt 3",
            "attempt 4",
            "thought the user"
    };

    private EmbeddingDescriptionSanitizer() {}

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!looksLikeChainOfThought(trimmed)) {
            return trimmed;
        }
        String[] paragraphs = trimmed.split("\\n\\n+");
        for (int i = paragraphs.length - 1; i >= 0; i--) {
            String candidate = paragraphs[i].trim();
            if (isClinicalNarrative(candidate)) {
                return candidate;
            }
        }
        String[] lines = trimmed.split("\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String candidate = lines[i].trim();
            if (isClinicalNarrative(candidate)) {
                return candidate;
            }
        }
        return trimmed;
    }

    private static boolean looksLikeChainOfThought(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String marker : CHAIN_OF_THOUGHT_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClinicalNarrative(String candidate) {
        if (candidate.length() < 30 || candidate.length() > 4_000) {
            return false;
        }
        if (!Character.isLetter(candidate.charAt(0))) {
            return false;
        }
        return !looksLikeChainOfThought(candidate);
    }
}
