package com.berdachuk.medexpertmatch.llm.chat;

/**
 * Thread-local chat agent profile for tool scope enforcement during a turn.
 */
public final class ChatToolContextHolder {

    private static final ThreadLocal<ChatAgentProfile> PROFILE = new ThreadLocal<>();

    private ChatToolContextHolder() {}

    public static void setProfile(ChatAgentProfile profile) {
        PROFILE.set(profile);
    }

    public static ChatAgentProfile profileOrNull() {
        return PROFILE.get();
    }

    public static void clear() {
        PROFILE.remove();
    }
}
