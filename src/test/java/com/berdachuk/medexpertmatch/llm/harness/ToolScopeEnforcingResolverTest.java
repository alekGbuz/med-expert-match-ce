package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.ChatAgentProfile;
import com.berdachuk.medexpertmatch.llm.chat.ChatToolContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class ToolScopeEnforcingResolverTest {

    @AfterEach
    void clear() {
        ChatToolContextHolder.clear();
    }

    @Test
    @DisplayName("returns denied callback when tool outside profile scope")
    void deniesOutOfScopeTool() {
        ToolCallback allowed = mock(ToolCallback.class);
        ToolCallbackResolver delegate = name -> "search_clinical_guidelines".equals(name) ? allowed : null;
        ToolScopeEnforcingResolver resolver = new ToolScopeEnforcingResolver(delegate);

        ChatToolContextHolder.setProfile(ChatAgentProfile.SPECIALIST_MATCHER);
        ToolCallback resolved = resolver.resolve("search_clinical_guidelines");

        assertNotNull(resolved);
        assertInstanceOf(DeniedToolCallback.class, resolved);
    }
}
