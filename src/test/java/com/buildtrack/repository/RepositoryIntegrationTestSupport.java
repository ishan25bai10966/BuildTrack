package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.Project;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

abstract class RepositoryIntegrationTestSupport {

    @BeforeAll
    static void requireDatabaseConfiguration() {
        Assumptions.assumeTrue(DatabaseManager.isConfigured(),
                "Set BUILDTRACK_DB_PASSWORD to run repository integration tests.");
    }

    protected Project createProject() throws Exception {
        Project project = new HardwareProject(0, uniqueName("project"), "Test project",
                LocalDate.now(), LocalDate.now().plusDays(14), new BigDecimal("100.00"));
        new ProjectRepository().save(project);
        return project;
    }

    protected void deleteProject(Project project) throws Exception {
        new ProjectRepository().delete(project.getProjectId());
    }

    protected String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
