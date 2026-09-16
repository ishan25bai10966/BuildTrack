package com.buildtrack.service;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.FeasibilityResult;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.Project;
import com.buildtrack.repository.BuildStepRepository;
import com.buildtrack.repository.ProjectRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FeasibilityServiceTest {

    @Test
    void classifiesHealthScoreBoundaries() {
        assertEquals(FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityService.classifyHealthScore(100));
        assertEquals(FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityService.classifyHealthScore(76));
        assertEquals(FeasibilityResult.FeasibilityStatus.AT_RISK,
                FeasibilityService.classifyHealthScore(75));
        assertEquals(FeasibilityResult.FeasibilityStatus.AT_RISK,
                FeasibilityService.classifyHealthScore(46));
        assertEquals(FeasibilityResult.FeasibilityStatus.CRITICAL,
                FeasibilityService.classifyHealthScore(45));
        assertEquals(FeasibilityResult.FeasibilityStatus.CRITICAL,
                FeasibilityService.classifyHealthScore(0));
    }

    @Test
    void throwsExceptionWhenProjectDoesNotExist() {
        ProjectService dummyProjectService = new ProjectService(new ProjectRepository() {
            @Override
            public Project findById(int projectId) {
                return null;
            }
        });
        FeasibilityService service = new FeasibilityService(
                dummyProjectService,
                new BuildStepService(),
                new BudgetService(),
                new InventoryService()
        );

        assertThrows(IllegalArgumentException.class, () -> service.analyzeProject(999999));
    }

    @Test
    void analyzesProjectWhenDatabaseConfigured() throws Exception {
        Assumptions.assumeTrue(DatabaseManager.isConfigured(),
                "Set BUILDTRACK_DB_PASSWORD to run feasibility integration tests.");

        ProjectRepository projectRepository = new ProjectRepository();
        BuildStepRepository stepRepository = new BuildStepRepository();
        FeasibilityService feasibilityService = new FeasibilityService();

        Project project = new HardwareProject(
                0,
                "FeasibilityTest-" + UUID.randomUUID(),
                "Feasibility test project",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("5000.00")
        );
        int projectId = projectRepository.save(project);

        BuildStep step = new BuildStep(
                0,
                projectId,
                "Initial Setup",
                "Setup hardware components",
                10.0,
                0.0,
                LocalDate.now().plusDays(10),
                BuildStep.Status.PENDING,
                BuildStep.Priority.MEDIUM
        );
        int stepId = stepRepository.save(step);

        try {
            FeasibilityResult result = feasibilityService.analyzeProject(projectId);
            assertNotNull(result);
            assertEquals(project.getName(), result.projectName());
            assertTrue(result.healthScore() >= 0 && result.healthScore() <= 100);
            assertNotNull(result.overallStatus());
            assertNotNull(result.recommendations());
            assertFalse(result.recommendations().isEmpty());
            assertEquals(10.0, result.totalEstimatedHours());
        } finally {
            stepRepository.delete(stepId);
            projectRepository.delete(projectId);
        }
    }
}

