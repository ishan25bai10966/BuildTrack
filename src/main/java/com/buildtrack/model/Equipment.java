package com.buildtrack.model;

public class Equipment {

    private int equipmentId;
    private String name;
    private String category;
    private boolean available;

    public Equipment() {
    }

    public Equipment(int equipmentId, String name,
                     String category, boolean available) {

        this.equipmentId = equipmentId;
        this.name = name;
        this.category = category;
        this.available = available;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}