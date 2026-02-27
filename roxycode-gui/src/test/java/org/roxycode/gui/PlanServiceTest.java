package org.roxycode.gui;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MicronautTest
class PlanServiceTest {

    @Inject
    PlanService planService;

    @Inject
    ProjectService projectService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        // Need to set project path in projectService if it's not a mock
        // Assuming ProjectService can be updated or mocked. 
        // For simplicity in this test, we assume planService uses the real ProjectService
        // and we need to point it to tempDir.
        projectService.setProjectPath(tempDir.toString());
    }

    @Test
    void testCreateAndLoadPlan() {
        planService.createPlan(
            "test-plan",
            "Test Goal",
            List.of("Req 1"),
            List.of("Con 1"),
            List.of("Step 1")
        );

        Plan plan = planService.loadActivePlan();
        assertNotNull(plan);
        assertEquals("test-plan", plan.name());
        assertEquals("PLANNING", plan.status());
        assertEquals(AgentRole.ANALYST, plan.activeRole());
        // Check that quality gates were added
        assertTrue(plan.implementationSteps().size() > 1);
    }

    @Test
    void testApprovePlan() {
        planService.createPlan("p1", "g", List.of(), List.of(), List.of());
        planService.approvePlan();
        
        Plan plan = planService.loadActivePlan();
        assertEquals("IMPLEMENTATION", plan.status());
        assertEquals(AgentRole.CODER, plan.activeRole());
        assertTrue(plan.isUserApproved());
    }

    @Test
    void testCompleteStep() {
        planService.createPlan("p1", "g", List.of(), List.of(), List.of("Task 1"));
        planService.completeStep("Task 1");
        
        Plan plan = planService.loadActivePlan();
        TaskItem item = plan.implementationProgress().stream()
                .filter(i -> i.text().equals("Task 1"))
                .findFirst().orElseThrow();
        assertTrue(item.completed());
    }
}
