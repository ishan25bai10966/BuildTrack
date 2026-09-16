package com.buildtrack.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class IoTProject extends HardwareProject {

    public IoTProject() {
        super();
    }

    public IoTProject(int projectId, String name, String description,
                      LocalDate startDate, LocalDate deadline, BigDecimal budget) {

        super(projectId, name, description, startDate, deadline, budget);
    }

    @Override
    public BigDecimal calculateEstimatedCost() {
        // IoT projects may require additional electronics and sensor costs.
        return getBudget().multiply(new BigDecimal("1.10"));
    }

    @Override
    public int calculateEstimatedDuration() {
        // IoT projects include additional integration/configuration time.
        return super.calculateEstimatedDuration() + 2;
    }
}
