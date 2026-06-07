package com.berdachuk.medexpertmatch.llm.harness;

/**
 * Shapes verbose harness/tool payloads into compact structured context before T3 clinical LLM calls (M68).
 */
public interface HarnessContextSummarizer {

    String summarizeToolResults(String rawToolResults, HarnessContextKind kind);
}
