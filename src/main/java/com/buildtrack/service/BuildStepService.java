package com.buildtrack.service;

import com.buildtrack.exceptions.DependencyException;
import com.buildtrack.exceptions.InvalidDeadlineException;
import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.Project;
import com.buildtrack.model.TaskDependency;
import com.buildtrack.repository.BuildStepRepository;
import com.buildtrack.repository.ProjectRepository;
import com.buildtrack.repository.TaskDependencyRepository;

import java.time.LocalDate;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildStepService {
    private final BuildStepRepository buildStepRepository;
    private final ProjectRepository projectRepository;
    private final TaskDependencyRepository dependencyRepository;

    public BuildStepService() {
        this(new BuildStepRepository(), new ProjectRepository(), new TaskDependencyRepository());
    }

    public BuildStepService(BuildStepRepository buildStepRepository,
                            ProjectRepository projectRepository,
                            TaskDependencyRepository dependencyRepository) {
        this.buildStepRepository = buildStepRepository;
        this.projectRepository = projectRepository;
        this.dependencyRepository = dependencyRepository;
    }

    public int createBuildStep(BuildStep step) throws SQLException {
        validateStep(step);
        return buildStepRepository.save(step);
    }

    public void updateBuildStep(BuildStep step) throws SQLException {
        validateStep(step);
        validateStartAllowed(step);
        buildStepRepository.update(step);
    }

    public void changeStatus(int stepId, BuildStep.Status status) throws SQLException {
        BuildStep step = requireStep(stepId);
        step.setStatus(status);
        updateBuildStep(step);
    }

    public void deleteBuildStep(int stepId) throws SQLException {
        buildStepRepository.delete(stepId);
    }

    public List<BuildStep> findBuildSteps(int projectId) throws SQLException {
        return buildStepRepository.findByProjectId(projectId);
    }

    public double calculateProjectProgress(int projectId) throws SQLException {
        return calculateProgress(buildStepRepository.findByProjectId(projectId));
    }

    public static double calculateProgress(List<BuildStep> buildSteps) {
        List<BuildStep> steps = buildSteps.stream()
                .filter(step -> step.getStatus() != BuildStep.Status.CANCELLED).toList();
        if (steps.isEmpty()) return 0.0;
        double totalHours = steps.stream().mapToDouble(BuildStep::getEstimatedHours).sum();
        if (totalHours <= 0) {
            return steps.stream().filter(step -> step.getStatus() == BuildStep.Status.COMPLETED).count()
                    * 100.0 / steps.size();
        }
        double completedHours = steps.stream()
                .filter(step -> step.getStatus() == BuildStep.Status.COMPLETED)
                .mapToDouble(BuildStep::getEstimatedHours).sum();
        return completedHours * 100.0 / totalHours;
    }

    public List<BuildStep> findOverdueSteps(int projectId, LocalDate today) throws SQLException {
        return buildStepRepository.findByProjectId(projectId).stream()
                .filter(step -> step.getDeadline() != null && step.getDeadline().isBefore(today))
                .filter(step -> step.getStatus() != BuildStep.Status.COMPLETED
                        && step.getStatus() != BuildStep.Status.CANCELLED).toList();
    }

    public void addDependency(int stepId, int prerequisiteStepId) throws SQLException {
        validateDependencyIds(stepId, prerequisiteStepId);
        BuildStep step = requireStep(stepId);
        BuildStep prerequisite = requireStep(prerequisiteStepId);
        if (step.getProjectId() != prerequisite.getProjectId()) {
            throw new DependencyException("Dependent steps must belong to the same project.");
        }
        if (dependencyRepository.findByStepId(stepId).stream()
                .anyMatch(dependency -> dependency.getPrerequisiteStepId() == prerequisiteStepId)) {
            throw new DependencyException("This dependency already exists.");
        }
        if (hasDependencyPath(prerequisiteStepId, stepId, new HashSet<>())) {
            throw new DependencyException("This dependency would create a circular chain.");
        }
        dependencyRepository.save(new TaskDependency(stepId, prerequisiteStepId));
    }

    public static void validateDependencyIds(int stepId, int prerequisiteStepId) {
        if (stepId <= 0 || prerequisiteStepId <= 0) {
            throw new DependencyException("Build-step IDs must be positive.");
        }
        if (stepId == prerequisiteStepId) {
            throw new DependencyException("A build step cannot depend on itself.");
        }
    }

    public List<TaskDependency> findDependencies(int stepId) throws SQLException {
        return dependencyRepository.findByStepId(stepId);
    }

    public boolean hasIncompletePrerequisites(int stepId) throws SQLException {
        for (TaskDependency dependency : dependencyRepository.findByStepId(stepId)) {
            if (requireStep(dependency.getPrerequisiteStepId()).getStatus() != BuildStep.Status.COMPLETED) {
                return true;
            }
        }
        return false;
    }

    private void validateStep(BuildStep step) throws SQLException {
        if (step == null || step.getName() == null || step.getName().isBlank()) {
            throw new InvalidProjectException("Build step name is required.");
        }
        if (step.getEstimatedHours() < 0 || step.getActualHours() < 0) {
            throw new InvalidProjectException("Build-step hours cannot be negative.");
        }
        Project project = projectRepository.findById(step.getProjectId());
        if (project == null) throw new InvalidProjectException("Project does not exist.");
        if (step.getDeadline() != null && project.getDeadline() != null
                && step.getDeadline().isAfter(project.getDeadline())) {
            throw new InvalidDeadlineException("Build-step deadline cannot be after the project deadline.");
        }
    }

    private void validateStartAllowed(BuildStep step) throws SQLException {
        if (step.getStatus() == BuildStep.Status.IN_PROGRESS && hasIncompletePrerequisites(step.getStepId())) {
            throw new DependencyException("A build step cannot start until all prerequisites are completed.");
        }
    }

    private BuildStep requireStep(int stepId) throws SQLException {
        BuildStep step = buildStepRepository.findById(stepId);
        if (step == null) throw new DependencyException("Build step " + stepId + " does not exist.");
        return step;
    }

    private boolean hasDependencyPath(int currentStepId, int targetStepId,
                                      Set<Integer> visited) throws SQLException {
        if (!visited.add(currentStepId)) return false;
        if (currentStepId == targetStepId) return true;
        for (TaskDependency dependency : dependencyRepository.findByStepId(currentStepId)) {
            if (hasDependencyPath(dependency.getPrerequisiteStepId(), targetStepId, visited)) return true;
        }
        return false;
    }
}
