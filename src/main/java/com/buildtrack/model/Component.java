package com.buildtrack.model;

import java.math.BigDecimal;

public class Component {

    private int componentId;
    private String name;
    private String category;
    private String unit;
    private BigDecimal unitCost;

    public Component() {
    }

    public Component(int componentId, String name, String category,
                     String unit, BigDecimal unitCost) {

        this.componentId = componentId;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.unitCost = unitCost;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }
}
