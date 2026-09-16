package com.buildtrack.model;

import java.time.LocalDate;

public class BuildStep {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        BLOCKED,
        CANCELLED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private int stepId;
    private int projectId;
    private String name;
    private String description;
    private double estimatedHours;
    private double actualHours;
    private LocalDate deadline;
    private Status status;
    private Priority priority;

    public BuildStep() {
        this.status = Status.PENDING;
        this.priority = Priority.MEDIUM;
    }

    public BuildStep(int stepId, int projectId, String name,
                     String description, double estimatedHours,
                     double actualHours, LocalDate deadline,
                     Status status, Priority priority) {

        this.stepId = stepId;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.estimatedHours = estimatedHours;
        this.actualHours = actualHours;
        this.deadline = deadline;
        this.status = status;
        this.priority = priority;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

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

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public double getActualHours() {
        return actualHours;
    }

    public void setActualHours(double actualHours) {
        this.actualHours = actualHours;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}