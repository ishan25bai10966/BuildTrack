package com.buildtrack.repository;

import com.buildtrack.model.BuildStep;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildStepRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesBuildStep() throws Exception {

        BuildStepRepository repository = new BuildStepRepository();

        var project = createProject();
        BuildStep step = new BuildStep(
                0,
                project.getProjectId(),
                uniqueName("step"),
                "Test step",
                3.0,
                0.0,
                LocalDate.now().plusDays(5),
                BuildStep.Status.PENDING,
                BuildStep.Priority.HIGH
        );

        int stepId = repository.save(step);
        try {
            assertEquals(stepId, step.getStepId());
            step.setStatus(BuildStep.Status.COMPLETED);
            repository.update(step);
            assertEquals(BuildStep.Status.COMPLETED, repository.findById(stepId).getStatus());
        } finally {
            repository.delete(stepId);
            deleteProject(project);
        }
        assertNull(repository.findById(stepId));
    }
}
