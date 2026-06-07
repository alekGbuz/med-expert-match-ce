package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.util.LlmDateTimeContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Exposes server clock to the agent for timelines, scheduling, and time-sensitive reasoning.
 */
@Component
public class DateTimeAgentTools {

    @Tool(description = "Returns the current date and time in UTC. Use for scheduling, timelines, and time-sensitive clinical context.")
    public String get_current_date_time() {
        return LlmDateTimeContext.contextBlock();
    }
}
