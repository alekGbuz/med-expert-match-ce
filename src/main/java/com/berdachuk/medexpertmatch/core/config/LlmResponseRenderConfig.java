package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.util.LlmResponseSanitizer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@code medexpertmatch.llm.response.render-embedded-json}
 * property (default {@code true}) into {@link LlmResponseSanitizer}'s
 * static toggle for the M74 embedded-JSON renderer.
 */
@Configuration
public class LlmResponseRenderConfig {

    private final boolean renderEmbeddedJson;

    public LlmResponseRenderConfig(
            @Value("${medexpertmatch.llm.response.render-embedded-json:true}") boolean renderEmbeddedJson) {
        this.renderEmbeddedJson = renderEmbeddedJson;
    }

    @PostConstruct
    void apply() {
        LlmResponseSanitizer.setRenderEmbeddedJson(renderEmbeddedJson);
    }
}
