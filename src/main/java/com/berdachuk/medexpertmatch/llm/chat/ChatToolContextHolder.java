package com.berdachuk.medexpertmatch.llm.chat;

/**
 * Thread-local chat agent profile for tool scope enforcement during a turn.
 */
public final class ChatToolContextHolder {

    private static final ThreadLocal<ChatAgentProfile> PROFILE = new ThreadLocal<>();
    private static final ThreadLocal<GoalType> GOAL_TYPE = new ThreadLocal<>();

    private ChatToolContextHolder() {}

    public static void setProfile(ChatAgentProfile profile) {
        PROFILE.set(profile);
    }

    public static void setGoalType(GoalType goalType) {
        if (goalType == null) {
            GOAL_TYPE.remove();
        } else {
            GOAL_TYPE.set(goalType);
        }
    }

    public static ChatAgentProfile profileOrNull() {
        return PROFILE.get();
    }

    public static GoalType goalTypeOrNull() {
        return GOAL_TYPE.get();
    }

    public static void clear() {
        PROFILE.remove();
        GOAL_TYPE.remove();
    }
}
