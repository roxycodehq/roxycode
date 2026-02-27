package org.roxycode.gui;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.genai.types.Content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a stateful Agent persona for chatting.
 */
@JsonDeserialize(builder = Agent.Builder.class)
public class Agent {
    private final String id;
    private final String name;
    private final String systemPrompt;
    private final AgentRole role;
    private final int maxWindowSize;
    private final Map<String, Object> session = new HashMap<>();

    private Agent(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.systemPrompt = builder.systemPrompt;
        this.role = builder.role != null ? builder.role : AgentRole.GENERAL;
        this.maxWindowSize = builder.maxWindowSize;
        this.session.put("history", new ArrayList<Content>());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSystemPrompt() { return systemPrompt; }
    public AgentRole getRole() { return role; }
    public int getMaxWindowSize() { return maxWindowSize; }
    public Map<String, Object> getSession() { return session; }

    @SuppressWarnings("unchecked")
    public List<Content> getHistory() {
        return (List<Content>) session.get("history");
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        @JsonProperty("id")
        private String id;
        @JsonProperty("name")
        private String name;
        @JsonProperty("system_prompt")
        private String systemPrompt;
        @JsonProperty("role")
        private AgentRole role;
        @JsonProperty("max_window_size")
        private int maxWindowSize = 20;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder role(AgentRole role) { this.role = role; return this; }
        public Builder maxWindowSize(int maxWindowSize) { this.maxWindowSize = maxWindowSize; return this; }

        public Agent build() {
            if (id == null) throw new IllegalStateException("Agent id is required");
            return new Agent(this);
        }
    }
}