package com.buildtrack.model;

import java.math.BigDecimal;

public record BudgetSummary(BigDecimal projectBudget, BigDecimal estimatedComponentCost,
                            BigDecimal recordedExpenses, BigDecimal totalRequired,
                            BigDecimal remainingBudget, BigDecimal utilizationPercent,
                            BudgetStatus status) {
    public enum BudgetStatus { NORMAL, WARNING, CRITICAL, OVER_BUDGET }
}
