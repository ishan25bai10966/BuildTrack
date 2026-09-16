package com.buildtrack.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public abstract class Project {

    private int projectId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate deadline;
    private BigDecimal budget;

    public Project() {
    }

    public Project(int projectId, String name, String description,
                   LocalDate startDate, LocalDate deadline, BigDecimal budget) {

        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.deadline = deadline;
        this.budget = budget;
    }

    public abstract BigDecimal calculateEstimatedCost();

    public abstract int calculateEstimatedDuration();

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
}
