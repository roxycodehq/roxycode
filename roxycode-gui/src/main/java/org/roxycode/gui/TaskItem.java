package org.roxycode.gui;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A granular task item in a plan.
 */
public record TaskItem(
    @JsonProperty("text") String text,
    @JsonProperty("completed") boolean completed
) {}