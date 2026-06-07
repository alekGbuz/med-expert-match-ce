package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Per-tier token budgets for cost-quality routing (M64).
 */
@Validated
@ConfigurationProperties(prefix = "medexpertmatch.llm.tier")
public record LlmTierProperties(
        TierBudget light,
        TierBudget standard,
        TierBudget full) {

    public LlmTierProperties {
        if (light == null) {
            light = TierBudget.lightDefault();
        }
        if (standard == null) {
            standard = TierBudget.standardDefault();
        }
        if (full == null) {
            full = TierBudget.fullDefault();
        }
    }

    public record TierBudget(int maxTokens) {

        static TierBudget lightDefault() {
            return new TierBudget(2048);
        }

        static TierBudget standardDefault() {
            return new TierBudget(4096);
        }

        static TierBudget fullDefault() {
            return new TierBudget(6000);
        }
    }

    public int maxTokensFor(RoutingTier tier) {
        return switch (tier) {
            case LIGHT -> light.maxTokens();
            case STANDARD -> standard.maxTokens();
            case FULL -> full.maxTokens();
        };
    }
}
