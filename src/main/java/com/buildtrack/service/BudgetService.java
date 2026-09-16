package com.buildtrack.service;

import com.buildtrack.exceptions.BudgetExceededException;
import com.buildtrack.model.BudgetSummary;
import com.buildtrack.model.Component;
import com.buildtrack.model.Expense;
import com.buildtrack.model.Project;
import com.buildtrack.model.ProjectComponent;
import com.buildtrack.repository.ComponentRepository;
import com.buildtrack.repository.ExpenseRepository;
import com.buildtrack.repository.ProjectComponentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

public class BudgetService {
    public static final BigDecimal WARNING_THRESHOLD = new BigDecimal("75");
    public static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("90");
    public static final BigDecimal OVER_BUDGET_THRESHOLD = new BigDecimal("100");

    private final ExpenseRepository expenseRepository;
    private final ProjectComponentRepository projectComponentRepository;
    private final ComponentRepository componentRepository;

    public BudgetService() {
        this(new ExpenseRepository(), new ProjectComponentRepository(), new ComponentRepository());
    }

    public BudgetService(ExpenseRepository expenseRepository,
                         ProjectComponentRepository projectComponentRepository,
                         ComponentRepository componentRepository) {
        this.expenseRepository = expenseRepository;
        this.projectComponentRepository = projectComponentRepository;
        this.componentRepository = componentRepository;
    }

    public BudgetSummary getBudgetSummary(Project project) throws SQLException {
        BigDecimal components = calculateEstimatedComponentCost(project.getProjectId());
        BigDecimal expenses = calculateRecordedExpenses(project.getProjectId());
        return calculateBudgetSummary(project, components, expenses);
    }

    public BigDecimal calculateRecordedExpenses(int projectId) throws SQLException {
        return expenseRepository.findByProjectId(projectId).stream().map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateEstimatedComponentCost(int projectId) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (ProjectComponent requirement : projectComponentRepository.findByProjectId(projectId)) {
            Component component = componentRepository.findById(requirement.getComponentId());
            if (component != null) {
                total = total.add(component.getUnitCost().multiply(
                        BigDecimal.valueOf(requirement.getQuantityRequired())));
            }
        }
        return total;
    }

    public static BudgetSummary calculateBudgetSummary(Project project,
                                                       BigDecimal estimatedComponentCost,
                                                       BigDecimal recordedExpenses) {
        BigDecimal budget = project.getBudget();
        BigDecimal required = estimatedComponentCost.add(recordedExpenses);
        BigDecimal remaining = budget.subtract(required);
        BigDecimal utilization = budget.signum() == 0
                ? (required.signum() == 0 ? BigDecimal.ZERO : new BigDecimal("100"))
                : required.multiply(new BigDecimal("100")).divide(budget, 2, RoundingMode.HALF_UP);
        return new BudgetSummary(budget, estimatedComponentCost, recordedExpenses, required,
                remaining, utilization, budgetStatus(utilization));
    }

    public void requireWithinBudget(BudgetSummary summary) {
        if (summary.status() == BudgetSummary.BudgetStatus.OVER_BUDGET) {
            throw new BudgetExceededException("Project is over budget by "
                    + summary.remainingBudget().abs() + ".");
        }
    }

    public static BudgetSummary.BudgetStatus budgetStatus(BigDecimal utilizationPercent) {
        if (utilizationPercent.compareTo(OVER_BUDGET_THRESHOLD) > 0) {
            return BudgetSummary.BudgetStatus.OVER_BUDGET;
        }
        if (utilizationPercent.compareTo(CRITICAL_THRESHOLD) > 0) {
            return BudgetSummary.BudgetStatus.CRITICAL;
        }
        if (utilizationPercent.compareTo(WARNING_THRESHOLD) >= 0) {
            return BudgetSummary.BudgetStatus.WARNING;
        }
        return BudgetSummary.BudgetStatus.NORMAL;
    }
}
