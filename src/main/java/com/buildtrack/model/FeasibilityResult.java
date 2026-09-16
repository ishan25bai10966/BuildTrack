package com.buildtrack.model;

import java.math.BigDecimal;
import java.util.List;

public record FeasibilityResult(String projectName, int healthScore,
                                FeasibilityStatus timeStatus,
                                FeasibilityStatus budgetStatus,
                                FeasibilityStatus componentStatus,
                                FeasibilityStatus dependencyStatus,
                                FeasibilityStatus deadlineStatus,
                                FeasibilityStatus overallStatus,
                                double totalEstimatedHours, long availableDays,
                                BigDecimal remainingBudget, int totalRequiredComponentQuantity,
                                int availableComponentQuantity, int missingComponentQuantity,
                                int blockedStepCount, int overdueStepCount,
                                List<String> recommendations) {
    public enum FeasibilityStatus { ON_TRACK, AT_RISK, CRITICAL }
}
