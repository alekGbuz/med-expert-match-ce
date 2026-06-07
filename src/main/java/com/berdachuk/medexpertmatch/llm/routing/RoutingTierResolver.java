package com.berdachuk.medexpertmatch.llm.routing;

import com.berdachuk.medexpertmatch.core.config.LlmTierProperties;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;

/**
 * Maps {@link GoalType} values to {@link RoutingTier} for cost-aware routing (M64).
 */
public final class RoutingTierResolver {

    private RoutingTierResolver() {
    }

    public static RoutingTier fromGoal(GoalType goalType) {
        return switch (goalType) {
            case GENERAL_QUESTION -> RoutingTier.LIGHT;
            case SEARCH_EVIDENCE, TRIAGE_INTAKE, GENERATE_RECOMMENDATIONS -> RoutingTier.STANDARD;
            case MATCH_DOCTORS, ROUTE_CASE, ANALYZE_CASE -> RoutingTier.FULL;
        };
    }

    public static RoutingTier fromClassification(GoalClassification classification) {
        return fromGoal(classification.goalType());
    }

    public static int maxTokensFor(RoutingTier tier, LlmTierProperties properties) {
        return switch (tier) {
            case LIGHT -> properties.light().maxTokens();
            case STANDARD -> properties.standard().maxTokens();
            case FULL -> properties.full().maxTokens();
        };
    }
}
