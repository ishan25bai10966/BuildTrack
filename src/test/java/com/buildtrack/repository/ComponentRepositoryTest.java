package com.buildtrack.repository;

import com.buildtrack.model.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesComponent() throws Exception {

        ComponentRepository repository = new ComponentRepository();

        Component component = new Component(
                0,
                uniqueName("component"),
                "Test",
                "piece",
                new BigDecimal("450.00")
        );

        int componentId = repository.save(component);
        try {
            assertEquals(componentId, component.getComponentId());
            assertEquals(component.getName(), repository.findById(componentId).getName());
            component.setUnitCost(new BigDecimal("500.00"));
            repository.update(component);
            assertEquals(new BigDecimal("500.00"), repository.findById(componentId).getUnitCost());
        } finally {
            repository.delete(componentId);
        }
        assertNull(repository.findById(componentId));
    }
}
