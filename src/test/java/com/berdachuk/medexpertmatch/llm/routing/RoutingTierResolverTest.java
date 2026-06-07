package com.berdachuk.medexpertmatch.llm.routing;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutingTierResolverTest {

    @ParameterizedTest
    @EnumSource(GoalType.class)
    @DisplayName("every GoalType maps to exactly one RoutingTier")
    void mapsEveryGoalType(GoalType goalType) {
        RoutingTier tier = RoutingTierResolver.fromGoal(goalType);
        assertEquals(expectedTier(goalType), tier);
    }

    @ParameterizedTest
    @EnumSource(value = GoalType.class, names = {"GENERAL_QUESTION"})
    @DisplayName("GENERAL_QUESTION is LIGHT — must not use full harness budget")
    void generalQuestionIsLight(GoalType goalType) {
        GoalClassification classification = GoalClassification.general();
        assertEquals(RoutingTier.LIGHT, RoutingTierResolver.fromClassification(classification));
    }

    private static RoutingTier expectedTier(GoalType goalType) {
        return switch (goalType) {
            case GENERAL_QUESTION -> RoutingTier.LIGHT;
            case SEARCH_EVIDENCE, TRIAGE_INTAKE, GENERATE_RECOMMENDATIONS -> RoutingTier.STANDARD;
            case MATCH_DOCTORS, ROUTE_CASE, ANALYZE_CASE -> RoutingTier.FULL;
        };
    }
}
