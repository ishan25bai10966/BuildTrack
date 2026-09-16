package com.buildtrack.repository;

import com.buildtrack.model.BuildStep;
import com.buildtrack.model.TaskDependency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskDependencyRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsAndDeletesTaskDependency() throws Exception {
        var project = createProject();
        BuildStepRepository stepRepository = new BuildStepRepository();
        TaskDependencyRepository dependencyRepository = new TaskDependencyRepository();

        BuildStep step1 = new BuildStep(
                0, project.getProjectId(), uniqueName("step-prereq"),
                "Prerequisite step", 4.0, 0.0, LocalDate.now().plusDays(5),
                BuildStep.Status.PENDING, BuildStep.Priority.HIGH
        );
        BuildStep step2 = new BuildStep(
                0, project.getProjectId(), uniqueName("step-dependent"),
                "Dependent step", 6.0, 0.0, LocalDate.now().plusDays(10),
                BuildStep.Status.PENDING, BuildStep.Priority.MEDIUM
        );

        int stepId1 = stepRepository.save(step1);
        int stepId2 = stepRepository.save(step2);

        TaskDependency dependency = new TaskDependency(stepId2, stepId1);

        try {
            dependencyRepository.save(dependency);

            List<TaskDependency> dependencies = dependencyRepository.findByStepId(stepId2);
            assertEquals(1, dependencies.size());
            assertEquals(stepId1, dependencies.get(0).getPrerequisiteStepId());

            dependencyRepository.delete(stepId2, stepId1);
            assertTrue(dependencyRepository.findByStepId(stepId2).isEmpty());
        } finally {
            stepRepository.delete(stepId2);
            stepRepository.delete(stepId1);
            deleteProject(project);
        }
    }
}

