package com.buildtrack.model;

public class TaskDependency {
    private int stepId;
    private int prerequisiteStepId;

    public TaskDependency() {
    }

    public TaskDependency(int stepId, int prerequisiteStepId) {
        this.stepId = stepId;
        this.prerequisiteStepId = prerequisiteStepId;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public int getPrerequisiteStepId() {
        return prerequisiteStepId;
    }

    public void setPrerequisiteStepId(int prerequisiteStepId) {
        this.prerequisiteStepId = prerequisiteStepId;
    }
}
