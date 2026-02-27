package org.roxycode.gui;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an Agent persona for chatting.
 */
public record Agent(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("system_prompt") String systemPrompt
) {
    @Override
    public String toString() {
        return name;
    }
}
