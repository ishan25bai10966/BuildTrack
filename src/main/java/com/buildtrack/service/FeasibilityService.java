package com.buildtrack.service;

import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.ComponentAvailability;
import com.buildtrack.model.FeasibilityResult;
import com.buildtrack.model.Project;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeasibilityService {
    public static final int WORK_HOURS_PER_DAY = 8;
    public static final int UPCOMING_DEADLINE_DAYS = 7;

    private final ProjectService projectService;
    private final BuildStepService buildStepService;
    private final BudgetService budgetService;
    private final InventoryService inventoryService;

    public FeasibilityService() {
        this(new ProjectService(), new BuildStepService(), new BudgetService(), new InventoryService());
    }

    public FeasibilityService(ProjectService projectService, BuildStepService buildStepService,
                              BudgetService budgetService, InventoryService inventoryService) {
        this.projectService = projectService;
        this.buildStepService = buildStepService;
        this.budgetService = budgetService;
        this.inventoryService = inventoryService;
    }

    public FeasibilityResult analyzeProject(int projectId) throws java.sql.SQLException {
        Project project = projectService.findProject(projectId);
        if (project == null) throw new IllegalArgumentException("Project does not exist.");
        LocalDate today = LocalDate.now();
        List<BuildStep> steps = buildStepService.findBuildSteps(projectId);
        List<BuildStep> overdue = buildStepService.findOverdueSteps(projectId, today);
        ComponentAvailability availability = inventoryService.getProjectComponentAvailability(projectId);
        Map<Integer, Integer> missing = availability.missingQuantities();
        BudgetSummary budget = budgetService.getBudgetSummary(project);
        int blocked = countBlockedSteps(steps);
        long availableDays = project.getDeadline() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(today, project.getDeadline()));
        double estimatedHours = steps.stream().filter(s -> s.getStatus() != BuildStep.Status.CANCELLED)
                .mapToDouble(BuildStep::getEstimatedHours).sum();

        FeasibilityResult.FeasibilityStatus time = timeStatus(estimatedHours, availableDays, overdue.size());
        FeasibilityResult.FeasibilityStatus budgetStatus = budgetStatus(budget);
        FeasibilityResult.FeasibilityStatus components = missing.isEmpty()
                ? FeasibilityResult.FeasibilityStatus.ON_TRACK : FeasibilityResult.FeasibilityStatus.AT_RISK;
        FeasibilityResult.FeasibilityStatus dependencies = blocked == 0
                ? FeasibilityResult.FeasibilityStatus.ON_TRACK : FeasibilityResult.FeasibilityStatus.AT_RISK;
        FeasibilityResult.FeasibilityStatus deadline = deadlineStatus(project.getDeadline(), today, overdue.size());
        List<String> recommendations = recommendations(time, budgetStatus, components, dependencies,
                deadline, budget, missing, blocked, overdue.size());
        int score = healthScore(time, budgetStatus, components, dependencies, deadline);
        FeasibilityResult.FeasibilityStatus overall = overallStatus(score, time, budgetStatus,
                components, dependencies, deadline);
        return new FeasibilityResult(project.getName(), score, time, budgetStatus, components,
                dependencies, deadline, overall, estimatedHours, availableDays,
                budget.remainingBudget(), availability.totalRequiredQuantity(),
                availability.availableRequiredQuantity(),
                missing.values().stream().mapToInt(Integer::intValue).sum(),
                blocked, overdue.size(), List.copyOf(recommendations));
    }

    public static FeasibilityResult.FeasibilityStatus classifyHealthScore(int score) {
        if (score <= 45) return FeasibilityResult.FeasibilityStatus.CRITICAL;
        if (score <= 75) return FeasibilityResult.FeasibilityStatus.AT_RISK;
        return FeasibilityResult.FeasibilityStatus.ON_TRACK;
    }

    private FeasibilityResult.FeasibilityStatus overallStatus(int score,
                                                               FeasibilityResult.FeasibilityStatus... statuses) {
        for (FeasibilityResult.FeasibilityStatus status : statuses) {
            if (status == FeasibilityResult.FeasibilityStatus.CRITICAL) {
                return FeasibilityResult.FeasibilityStatus.CRITICAL;
            }
        }
        return classifyHealthScore(score);
    }

    private int countBlockedSteps(List<BuildStep> steps) throws java.sql.SQLException {
        int blocked = 0;
        for (BuildStep step : steps) {
            if (step.getStatus() == BuildStep.Status.BLOCKED
                    || (step.getStatus() == BuildStep.Status.IN_PROGRESS
                    && buildStepService.hasIncompletePrerequisites(step.getStepId()))) blocked++;
        }
        return blocked;
    }

    private FeasibilityResult.FeasibilityStatus timeStatus(double hours, long days, int overdue) {
        if (overdue > 0 || hours > days * WORK_HOURS_PER_DAY) return FeasibilityResult.FeasibilityStatus.CRITICAL;
        if (hours > days * WORK_HOURS_PER_DAY * 0.8) return FeasibilityResult.FeasibilityStatus.AT_RISK;
        return FeasibilityResult.FeasibilityStatus.ON_TRACK;
    }

    private FeasibilityResult.FeasibilityStatus budgetStatus(BudgetSummary budget) {
        return switch (budget.status()) {
            case NORMAL -> FeasibilityResult.FeasibilityStatus.ON_TRACK;
            case WARNING -> FeasibilityResult.FeasibilityStatus.AT_RISK;
            case CRITICAL, OVER_BUDGET -> FeasibilityResult.FeasibilityStatus.CRITICAL;
        };
    }

    private FeasibilityResult.FeasibilityStatus deadlineStatus(LocalDate deadline, LocalDate today, int overdue) {
        if (deadline == null || deadline.isBefore(today) || overdue > 0) return FeasibilityResult.FeasibilityStatus.CRITICAL;
        if (!deadline.isAfter(today.plusDays(UPCOMING_DEADLINE_DAYS))) return FeasibilityResult.FeasibilityStatus.AT_RISK;
        return FeasibilityResult.FeasibilityStatus.ON_TRACK;
    }

    private int healthScore(FeasibilityResult.FeasibilityStatus... statuses) {
        int score = 100;
        for (FeasibilityResult.FeasibilityStatus status : statuses) {
            score -= status == FeasibilityResult.FeasibilityStatus.CRITICAL ? 25
                    : status == FeasibilityResult.FeasibilityStatus.AT_RISK ? 12 : 0;
        }
        return Math.max(0, score);
    }

    private List<String> recommendations(FeasibilityResult.FeasibilityStatus time,
                                         FeasibilityResult.FeasibilityStatus budgetStatus,
                                         FeasibilityResult.FeasibilityStatus components,
                                         FeasibilityResult.FeasibilityStatus dependencies,
                                         FeasibilityResult.FeasibilityStatus deadline,
                                         BudgetSummary budget, Map<Integer, Integer> missing,
                                         int blocked, int overdue) {
        List<String> messages = new ArrayList<>();
        if (time != FeasibilityResult.FeasibilityStatus.ON_TRACK) messages.add("Review the build schedule and estimated hours.");
        if (budgetStatus != FeasibilityResult.FeasibilityStatus.ON_TRACK) messages.add("Budget utilization is " + budget.utilizationPercent() + "%.");
        if (!missing.isEmpty()) messages.add("Purchase " + missing.values().stream().mapToInt(Integer::intValue).sum() + " missing component units before dependent work starts.");
        if (blocked > 0 || dependencies != FeasibilityResult.FeasibilityStatus.ON_TRACK) messages.add("Complete prerequisite steps before starting blocked work.");
        if (overdue > 0) messages.add(overdue + " build step(s) are overdue.");
        if (deadline == FeasibilityResult.FeasibilityStatus.AT_RISK) messages.add("The project deadline is within " + UPCOMING_DEADLINE_DAYS + " days.");
        if (messages.isEmpty()) messages.add("Project is currently on track. Continue monitoring progress and costs.");
        return messages;
    }
}
