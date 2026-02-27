package org.roxycode.gui;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to manage project plans and their lifecycle.
 */
@Singleton
public class PlanService {
    private static final Logger LOG = LoggerFactory.getLogger(PlanService.class);
    private final ProjectService projectService;
    private final TomlMapper tomlMapper = new TomlMapper();

    @Inject
    public PlanService(ProjectService projectService) {
        this.projectService = projectService;
    }

    private Path getActivePlanPath() {
        String projectPath = projectService.getProjectPath();
        if (projectPath == null) return null;
        return Paths.get(projectPath, "roxy", "plans", "active_plan.toml");
    }

    public Plan loadActivePlan() {
        Path path = getActivePlanPath();
        if (path == null || !Files.exists(path)) return null;
        try {
            return tomlMapper.readValue(path.toFile(), Plan.class);
        } catch (IOException e) {
            LOG.error("Failed to load active plan", e);
            return null;
        }
    }

    public void savePlan(Plan plan) {
        Path path = getActivePlanPath();
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            tomlMapper.writeValue(path.toFile(), plan);
        } catch (IOException e) {
            LOG.error("Failed to save plan", e);
        }
    }

    public void createPlan(String name, String goal, List<String> functionalRequirements, List<String> technicalConstraints, List<String> implementationSteps) {
        List<String> stepsWithGates = new ArrayList<>(implementationSteps);
        stepsWithGates.add("Add unit tests for new/modified logic");
        stepsWithGates.add("Verify all project tests pass");

        List<TaskItem> progress = stepsWithGates.stream()
                .map(step -> new TaskItem(step, false))
                .collect(Collectors.toList());

        Plan plan = new Plan(
                name,
                "PLANNING",
                false,
                goal,
                functionalRequirements,
                technicalConstraints,
                stepsWithGates,
                progress,
                "",
                AgentRole.ANALYST
        );
        savePlan(plan);
    }

    public void approvePlan() {
        Plan plan = loadActivePlan();
        if (plan != null && "PLANNING".equals(plan.status())) {
            Plan updated = new Plan(
                    plan.name(),
                    "IMPLEMENTATION",
                    true,
                    plan.goal(),
                    plan.functionalRequirements(),
                    plan.technicalConstraints(),
                    plan.implementationSteps(),
                    plan.implementationProgress(),
                    plan.qaFeedback(),
                    AgentRole.CODER
            );
            savePlan(updated);
        }
    }

    public void requestVerification() {
        Plan plan = loadActivePlan();
        if (plan != null && "IMPLEMENTATION".equals(plan.status())) {
            Plan updated = new Plan(
                    plan.name(),
                    "QA",
                    plan.isUserApproved(),
                    plan.goal(),
                    plan.functionalRequirements(),
                    plan.technicalConstraints(),
                    plan.implementationSteps(),
                    plan.implementationProgress(),
                    plan.qaFeedback(),
                    AgentRole.QA
            );
            savePlan(updated);
        }
    }

    public void rejectImplementation(String reason) {
        Plan plan = loadActivePlan();
        if (plan != null && "QA".equals(plan.status())) {
            Plan updated = new Plan(
                    plan.name(),
                    "IMPLEMENTATION",
                    plan.isUserApproved(),
                    plan.goal(),
                    plan.functionalRequirements(),
                    plan.technicalConstraints(),
                    plan.implementationSteps(),
                    plan.implementationProgress(),
                    reason,
                    AgentRole.CODER
            );
            savePlan(updated);
        }
    }

    public void completeStep(String taskText) {
        Plan plan = loadActivePlan();
        if (plan == null) return;

        List<TaskItem> updatedProgress = plan.implementationProgress().stream()
                .map(item -> item.text().equals(taskText) ? new TaskItem(item.text(), true) : item)
                .collect(Collectors.toList());

        Plan updated = new Plan(
                plan.name(),
                plan.status(),
                plan.isUserApproved(),
                plan.goal(),
                plan.functionalRequirements(),
                plan.technicalConstraints(),
                plan.implementationSteps(),
                updatedProgress,
                plan.qaFeedback(),
                plan.activeRole()
        );
        savePlan(updated);
    }

    public void completePlan() {
        archivePlan("completed");
    }

    public void cancelPlan() {
        archivePlan("canceled");
    }

    private void archivePlan(String subDir) {
        Plan plan = loadActivePlan();
        if (plan == null) return;

        String projectPath = projectService.getProjectPath();
        Path archiveDir = Paths.get(projectPath, "roxy", "plans", "archive", subDir);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        Path targetPath = archiveDir.resolve(plan.name() + "_" + timestamp + ".toml");

        try {
            Files.createDirectories(archiveDir);
            Files.move(getActivePlanPath(), targetPath);
        } catch (IOException e) {
            LOG.error("Failed to archive plan", e);
        }
    }

    public Map<String, Object> getProjectConfig() {
        String projectPath = projectService.getProjectPath();
        if (projectPath == null) return new HashMap<>();
        Path configPath = Paths.get(projectPath, "roxy", "config", "config.toml");
        if (!Files.exists(configPath)) return new HashMap<>();
        try {
            return tomlMapper.readValue(configPath.toFile(), Map.class);
        } catch (IOException e) {
            LOG.error("Failed to read project config", e);
            return new HashMap<>();
        }
    }
}