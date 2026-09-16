package com.buildtrack.repository;

import com.buildtrack.model.IoTProject;
import com.buildtrack.model.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesProject() throws Exception {

        ProjectRepository repository = new ProjectRepository();

        Project project = new IoTProject(
                0,
                uniqueName("project"),
                "Repository test",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("2000.00")
        );

        int projectId = repository.save(project);
        try {
            assertEquals(projectId, project.getProjectId());
            assertInstanceOf(IoTProject.class, repository.findById(projectId));
            project.setName(uniqueName("updated-project"));
            repository.update(project);
            assertEquals(project.getName(), repository.findById(projectId).getName());
        } finally {
            repository.delete(projectId);
        }
        assertNull(repository.findById(projectId));
    }
}
