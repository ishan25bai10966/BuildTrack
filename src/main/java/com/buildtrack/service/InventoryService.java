package com.buildtrack.service;

import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.exceptions.ResourceUnavailableException;
import com.buildtrack.model.Component;
import com.buildtrack.model.ComponentAvailability;
import com.buildtrack.model.InventoryItem;
import com.buildtrack.model.ProjectComponent;
import com.buildtrack.model.PurchaseItem;
import com.buildtrack.repository.ComponentRepository;
import com.buildtrack.repository.InventoryRepository;
import com.buildtrack.repository.ProjectComponentRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryService {
    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final ProjectComponentRepository projectComponentRepository;

    public InventoryService() {
        this(new ComponentRepository(), new InventoryRepository(), new ProjectComponentRepository());
    }

    public InventoryService(ComponentRepository componentRepository,
                            InventoryRepository inventoryRepository,
                            ProjectComponentRepository projectComponentRepository) {
        this.componentRepository = componentRepository;
        this.inventoryRepository = inventoryRepository;
        this.projectComponentRepository = projectComponentRepository;
    }

    public void addToInventory(int componentId, int quantity) throws SQLException {
        if (quantity < 0) throw new InvalidProjectException("Inventory quantity cannot be negative.");
        if (componentRepository.findById(componentId) == null) {
            throw new InvalidProjectException("Component does not exist.");
        }
        InventoryItem item = inventoryByComponent().get(componentId);
        if (item == null) {
            inventoryRepository.save(new InventoryItem(0, componentId, quantity));
        } else {
            item.setQuantityAvailable(quantity);
            inventoryRepository.update(item);
        }
    }

    public void adjustAvailableQuantity(int componentId, int adjustment) throws SQLException {
        InventoryItem item = inventoryByComponent().get(componentId);
        if (item == null) throw new ResourceUnavailableException("Component is not in inventory.");
        int updatedQuantity = item.getQuantityAvailable() + adjustment;
        if (updatedQuantity < 0) throw new ResourceUnavailableException("Inventory cannot fall below zero.");
        item.setQuantityAvailable(updatedQuantity);
        inventoryRepository.update(item);
    }

    public Map<Integer, Integer> calculateMissingQuantities(int projectId) throws SQLException {
        return getProjectComponentAvailability(projectId).missingQuantities();
    }

    public ComponentAvailability getProjectComponentAvailability(int projectId) throws SQLException {
        List<ProjectComponent> requirements = projectComponentRepository.findByProjectId(projectId);
        Map<Integer, InventoryItem> inventory = inventoryByComponent();
        int totalRequired = requirements.stream().mapToInt(ProjectComponent::getQuantityRequired).sum();
        int available = requirements.stream().mapToInt(requirement -> {
            InventoryItem item = inventory.get(requirement.getComponentId());
            return item == null ? 0 : Math.min(item.getQuantityAvailable(), requirement.getQuantityRequired());
        }).sum();
        return new ComponentAvailability(totalRequired, available,
                calculateMissingQuantities(requirements, inventory));
    }

    public List<PurchaseItem> generatePurchaseList(int projectId) throws SQLException {
        Map<Integer, Integer> missing = calculateMissingQuantities(projectId);
        List<PurchaseItem> purchaseList = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : missing.entrySet()) {
            Component component = componentRepository.findById(entry.getKey());
            if (component != null) {
                purchaseList.add(new PurchaseItem(component.getComponentId(), component.getName(),
                        entry.getValue(), component.getUnitCost().multiply(BigDecimal.valueOf(entry.getValue()))));
            }
        }
        return purchaseList;
    }

    public static Map<Integer, Integer> calculateMissingQuantities(
            List<ProjectComponent> requirements, Map<Integer, InventoryItem> inventory) {
        Map<Integer, Integer> missing = new HashMap<>();
        for (ProjectComponent requirement : requirements) {
            InventoryItem item = inventory.get(requirement.getComponentId());
            int available = item == null ? 0 : item.getQuantityAvailable();
            int shortage = requirement.getQuantityRequired() - available;
            if (shortage > 0) missing.put(requirement.getComponentId(), shortage);
        }
        return missing;
    }

    private Map<Integer, InventoryItem> inventoryByComponent() throws SQLException {
        Map<Integer, InventoryItem> inventory = new HashMap<>();
        for (InventoryItem item : inventoryRepository.findAll()) {
            inventory.put(item.getComponentId(), item);
        }
        return inventory;
    }
}
