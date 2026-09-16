package com.buildtrack.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

public class HardwareProject extends Project {

    public HardwareProject() {
        super();
    }

    public HardwareProject(int projectId, String name, String description,
                           LocalDate startDate, LocalDate deadline, BigDecimal budget) {

        super(projectId, name, description, startDate, deadline, budget);
    }

    @Override
    public BigDecimal calculateEstimatedCost() {
        return getBudget();
    }

    @Override
    public int calculateEstimatedDuration() {

        if (getStartDate() == null || getDeadline() == null) {
            return 0;
        }

        return (int) ChronoUnit.DAYS.between(
                getStartDate(),
                getDeadline()
        );
    }
}
