package com.buildtrack.service;

import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.model.Component;
import com.buildtrack.model.ProjectComponent;
import com.buildtrack.repository.ComponentRepository;
import com.buildtrack.repository.ProjectComponentRepository;
import com.buildtrack.repository.ProjectRepository;

import java.sql.SQLException;
import java.util.List;

public class ComponentService {
    private final ComponentRepository componentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectComponentRepository projectComponentRepository;

    public ComponentService() {
        this(new ComponentRepository(), new ProjectRepository(), new ProjectComponentRepository());
    }

    public ComponentService(ComponentRepository componentRepository,
                            ProjectRepository projectRepository,
                            ProjectComponentRepository projectComponentRepository) {
        this.componentRepository = componentRepository;
        this.projectRepository = projectRepository;
        this.projectComponentRepository = projectComponentRepository;
    }

    public int createComponent(Component component) throws SQLException {
        validateComponent(component);
        return componentRepository.save(component);
    }

    public void updateComponent(Component component) throws SQLException {
        validateComponent(component);
        componentRepository.update(component);
    }

    public Component findComponent(int componentId) throws SQLException {
        return componentRepository.findById(componentId);
    }

    public List<Component> findAllComponents() throws SQLException {
        return componentRepository.findAll();
    }

    public void addProjectRequirement(ProjectComponent requirement) throws SQLException {
        validateRequirement(requirement);
        if (projectComponentRepository.findById(requirement.getProjectId(),
                requirement.getComponentId()) != null) {
            throw new InvalidProjectException("This component is already required by the project.");
        }
        projectComponentRepository.save(requirement);
    }

    public void updateProjectRequirement(ProjectComponent requirement) throws SQLException {
        validateRequirement(requirement);
        projectComponentRepository.update(requirement);
    }

    private void validateComponent(Component component) {
        if (component == null || component.getName() == null || component.getName().isBlank()
                || component.getCategory() == null || component.getCategory().isBlank()
                || component.getUnit() == null || component.getUnit().isBlank()
                || component.getUnitCost() == null || component.getUnitCost().signum() < 0) {
            throw new InvalidProjectException("Component name, category, unit, and non-negative cost are required.");
        }
    }

    private void validateRequirement(ProjectComponent requirement) throws SQLException {
        if (requirement == null || requirement.getQuantityRequired() <= 0) {
            throw new InvalidProjectException("Required component quantity must be greater than zero.");
        }
        if (projectRepository.findById(requirement.getProjectId()) == null) {
            throw new InvalidProjectException("Project does not exist.");
        }
        if (componentRepository.findById(requirement.getComponentId()) == null) {
            throw new InvalidProjectException("Component does not exist.");
        }
    }
}
