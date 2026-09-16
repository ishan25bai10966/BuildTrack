package com.buildtrack.repository;

import com.buildtrack.model.Component;
import com.buildtrack.model.ProjectComponent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProjectComponentRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesProjectComponent() throws Exception {
        var project = createProject();
        ComponentRepository componentRepository = new ComponentRepository();
        Component component = new Component(0, uniqueName("component"), "Test", "piece",
                new BigDecimal("1.00"));
        componentRepository.save(component);
        ProjectComponentRepository repository = new ProjectComponentRepository();
        ProjectComponent link = new ProjectComponent(project.getProjectId(),
                component.getComponentId(), 2);
        repository.save(link);
        try {
            assertNotNull(repository.findById(project.getProjectId(), component.getComponentId()));
            link.setQuantityRequired(5);
            repository.update(link);
            assertEquals(5, repository.findById(project.getProjectId(),
                    component.getComponentId()).getQuantityRequired());
        } finally {
            repository.delete(project.getProjectId(), component.getComponentId());
            deleteProject(project);
            componentRepository.delete(component.getComponentId());
        }
        assertNull(repository.findById(project.getProjectId(), component.getComponentId()));
    }
}
