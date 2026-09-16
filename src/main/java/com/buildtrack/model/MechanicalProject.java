package com.buildtrack.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class MechanicalProject extends Project {

    public MechanicalProject() {
        super();
    }

    public MechanicalProject(int projectId, String name, String description,
                             LocalDate startDate, LocalDate deadline, BigDecimal budget) {

        super(projectId, name, description, startDate, deadline, budget);
    }

    @Override
    public BigDecimal calculateEstimatedCost() {
        // Mechanical projects may require additional material costs.
        return getBudget().multiply(new BigDecimal("1.15"));
    }

    @Override
    public int calculateEstimatedDuration() {

        if (getStartDate() == null || getDeadline() == null) {
            return 0;
        }

        return (int) (java.time.temporal.ChronoUnit.DAYS.between(
                getStartDate(),
                getDeadline()
        ) + 3);
    }
}
