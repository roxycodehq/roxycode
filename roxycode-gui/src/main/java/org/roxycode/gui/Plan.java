package org.roxycode.gui;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a project plan and its detailed specification.
 */
public record Plan(
    @JsonProperty("name") String name,
    @JsonProperty("status") String status, // PLANNING, IMPLEMENTATION, QA
    @JsonProperty("isUserApproved") boolean isUserApproved,
    @JsonProperty("goal") String goal,
    @JsonProperty("functionalRequirements") List<String> functionalRequirements,
    @JsonProperty("technicalConstraints") List<String> technicalConstraints,
    @JsonProperty("implementationSteps") List<String> implementationSteps,
    @JsonProperty("implementationProgress") List<TaskItem> implementationProgress,
    @JsonProperty("qaFeedback") String qaFeedback,
    @JsonProperty("activeRole") AgentRole activeRole
) {}