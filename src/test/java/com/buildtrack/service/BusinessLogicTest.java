package com.buildtrack.service;

import com.buildtrack.exceptions.DependencyException;
import com.buildtrack.exceptions.InvalidDeadlineException;
import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.BuildStep;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.InventoryItem;
import com.buildtrack.model.ProjectComponent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BusinessLogicTest {

    @Test
    void validatesProjectNameBudgetAndDeadline() {
        ProjectService service = new ProjectService();
        HardwareProject project = new HardwareProject(0, "", "", LocalDate.now(),
                LocalDate.now().plusDays(1), BigDecimal.ONE);
        assertThrows(InvalidProjectException.class, () -> service.validateProject(project));

        project.setName("Valid");
        project.setDeadline(project.getStartDate().minusDays(1));
        assertThrows(InvalidDeadlineException.class, () -> service.validateProject(project));
    }

    @Test
    void calculatesProgressFromCompletedEstimatedHours() {
        BuildStep completed = new BuildStep(1, 1, "A", "", 6, 0, null,
                BuildStep.Status.COMPLETED, BuildStep.Priority.MEDIUM);
        BuildStep pending = new BuildStep(2, 1, "B", "", 4, 0, null,
                BuildStep.Status.PENDING, BuildStep.Priority.MEDIUM);
        assertEquals(60.0, BuildStepService.calculateProgress(List.of(completed, pending)));
    }

    @Test
    void calculatesInventoryShortages() {
        List<ProjectComponent> requirements = List.of(new ProjectComponent(1, 10, 3),
                new ProjectComponent(1, 20, 2));
        Map<Integer, InventoryItem> inventory = Map.of(10, new InventoryItem(1, 10, 1));
        assertEquals(Map.of(10, 2, 20, 2),
                InventoryService.calculateMissingQuantities(requirements, inventory));
    }

    @Test
    void assignsBudgetThresholds() {
        HardwareProject project = new HardwareProject(1, "P", "", LocalDate.now(),
                LocalDate.now().plusDays(1), new BigDecimal("100.00"));
        BudgetSummary summary = BudgetService.calculateBudgetSummary(project,
                new BigDecimal("80.00"), BigDecimal.ZERO);
        assertEquals(BudgetSummary.BudgetStatus.WARNING, summary.status());
        assertEquals(BudgetSummary.BudgetStatus.OVER_BUDGET,
                BudgetService.budgetStatus(new BigDecimal("101")));
    }

    @Test
    void classifiesFeasibilityAndRejectsSelfDependency() {
        assertEquals(com.buildtrack.model.FeasibilityResult.FeasibilityStatus.ON_TRACK,
                FeasibilityService.classifyHealthScore(76));
        assertEquals(com.buildtrack.model.FeasibilityResult.FeasibilityStatus.CRITICAL,
                FeasibilityService.classifyHealthScore(45));
        assertThrows(DependencyException.class,
                () -> BuildStepService.validateDependencyIds(2, 2));
    }
}
