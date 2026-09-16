package com.buildtrack.model;

public class InventoryItem {

    private int inventoryId;
    private int componentId;
    private int quantityAvailable;

    public InventoryItem() {
    }

    public InventoryItem(int inventoryId, int componentId,
                         int quantityAvailable) {

        this.inventoryId = inventoryId;
        this.componentId = componentId;
        this.quantityAvailable = quantityAvailable;
    }

    public int getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }

    public int getComponentId() {
        return componentId;
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
}