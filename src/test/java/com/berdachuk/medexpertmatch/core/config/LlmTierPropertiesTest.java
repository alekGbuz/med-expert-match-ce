package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmTierPropertiesTest {

    @Test
    @DisplayName("defaults provide sensible max-tokens per tier")
    void defaultsAreSensible() {
        LlmTierProperties props = new LlmTierProperties(null, null, null);

        assertEquals(2048, props.light().maxTokens());
        assertEquals(4096, props.standard().maxTokens());
        assertEquals(6000, props.full().maxTokens());
    }

    @Test
    @DisplayName("medexpertmatch.llm.tier.* binds from properties")
    void bindsFromProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("medexpertmatch.llm.tier.light.max-tokens", "1024");
        env.setProperty("medexpertmatch.llm.tier.standard.max-tokens", "3000");
        env.setProperty("medexpertmatch.llm.tier.full.max-tokens", "8000");

        Binder binder = new Binder(ConfigurationPropertySources.get(env));
        LlmTierProperties props = binder
                .bind("medexpertmatch.llm.tier", Bindable.of(LlmTierProperties.class))
                .get();

        assertEquals(1024, props.light().maxTokens());
        assertEquals(3000, props.standard().maxTokens());
        assertEquals(8000, props.full().maxTokens());
    }
}
