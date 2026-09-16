package com.buildtrack.repository;

import com.buildtrack.model.Component;
import com.buildtrack.model.InventoryItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InventoryRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesInventoryItem() throws Exception {
        ComponentRepository componentRepository = new ComponentRepository();
        Component component = new Component(0, uniqueName("component"), "Test", "piece",
                new BigDecimal("1.00"));
        componentRepository.save(component);
        InventoryRepository repository = new InventoryRepository();
        InventoryItem item = new InventoryItem(0, component.getComponentId(), 3);
        int inventoryId = repository.save(item);
        try {
            item.setQuantityAvailable(7);
            repository.update(item);
            assertEquals(7, repository.findById(inventoryId).getQuantityAvailable());
        } finally {
            repository.delete(inventoryId);
            componentRepository.delete(component.getComponentId());
        }
        assertNull(repository.findById(inventoryId));
    }
}
