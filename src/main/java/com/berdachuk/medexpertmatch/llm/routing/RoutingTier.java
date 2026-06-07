package com.berdachuk.medexpertmatch.llm.routing;

/**
 * Cost-quality tier for LLM routing (M64).
 * Maps goal types to token budget and observability buckets.
 */
public enum RoutingTier {

    /** General Q&A, minimal context — FunctionGemma / low token budget. */
    LIGHT,

    /** Evidence search, triage, recommendations — tools + retrieval slice. */
    STANDARD,

    /** Match, route, analyze — full harness + GraphRAG. */
    FULL
}
