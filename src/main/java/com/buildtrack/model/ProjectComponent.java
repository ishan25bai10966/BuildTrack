package com.buildtrack.model;

public class ProjectComponent {

    private int projectId;
    private int componentId;
    private int quantityRequired;

    public ProjectComponent() {
    }

    public ProjectComponent(int projectId, int componentId,
                            int quantityRequired) {
        this.projectId = projectId;
        this.componentId = componentId;
        this.quantityRequired = quantityRequired;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public int getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(int quantityRequired) {
        this.quantityRequired = quantityRequired;
    }
}