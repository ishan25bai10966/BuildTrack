package com.buildtrack.service;

import com.buildtrack.exceptions.InvalidDeadlineException;
import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.IoTProject;
import com.buildtrack.model.MechanicalProject;
import com.buildtrack.model.Project;
import com.buildtrack.repository.ProjectRepository;

import java.sql.SQLException;
import java.util.List;

public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService() {
        this(new ProjectRepository());
    }

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public int createProject(Project project) throws SQLException {
        validateProject(project);
        return projectRepository.save(project);
    }

    public void updateProject(Project project) throws SQLException {
        validateProject(project);
        projectRepository.update(project);
    }

    public Project findProject(int projectId) throws SQLException {
        return projectRepository.findById(projectId);
    }

    public List<Project> findAllProjects() throws SQLException {
        return projectRepository.findAll();
    }

    public void deleteProject(int projectId) throws SQLException {
        projectRepository.delete(projectId);
    }

    public void validateProject(Project project) {
        if (project == null) {
            throw new InvalidProjectException("Project is required.");
        }
        if (project.getName() == null || project.getName().isBlank()) {
            throw new InvalidProjectException("Project name cannot be blank.");
        }
        if (project.getStartDate() == null || project.getDeadline() == null) {
            throw new InvalidDeadlineException("Project start date and deadline are required.");
        }
        if (project.getDeadline().isBefore(project.getStartDate())) {
            throw new InvalidDeadlineException("Project deadline cannot be before its start date.");
        }
        if (project.getBudget() == null || project.getBudget().signum() < 0) {
            throw new InvalidProjectException("Project budget cannot be negative.");
        }
        if (!(project instanceof HardwareProject)
                && !(project instanceof MechanicalProject)
                && !(project instanceof IoTProject)) {
            throw new InvalidProjectException("Unsupported project type.");
        }
    }
}
