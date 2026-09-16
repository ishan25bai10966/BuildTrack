package com.buildtrack.repository;

import com.buildtrack.model.Equipment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentRepositoryTest extends RepositoryIntegrationTestSupport {

    @Test
    void savesFindsUpdatesAndDeletesEquipment() throws Exception {
        EquipmentRepository repository = new EquipmentRepository();
        Equipment equipment = new Equipment(0, uniqueName("equipment"), "Tools", true);
        int equipmentId = repository.save(equipment);
        try {
            assertTrue(repository.findById(equipmentId).isAvailable());
            equipment.setAvailable(false);
            repository.update(equipment);
            assertFalse(repository.findById(equipmentId).isAvailable());
        } finally {
            repository.delete(equipmentId);
        }
        assertNull(repository.findById(equipmentId));
    }
}
