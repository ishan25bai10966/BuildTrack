package com.buildtrack.service;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.exceptions.ResourceUnavailableException;
import com.buildtrack.model.Equipment;
import com.buildtrack.repository.EquipmentRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentServiceTest {

    @Test
    void validatesEquipmentInputs() {
        EquipmentService service = new EquipmentService();

        assertThrows(InvalidProjectException.class, () -> service.validateEquipment(null));

        Equipment noName = new Equipment(0, "", "Lab", true);
        assertThrows(InvalidProjectException.class, () -> service.validateEquipment(noName));

        Equipment noCategory = new Equipment(0, "Oscilloscope", "   ", true);
        assertThrows(InvalidProjectException.class, () -> service.validateEquipment(noCategory));
    }

    @Test
    void managesEquipmentLifecycleWhenConfigured() throws Exception {
        Assumptions.assumeTrue(DatabaseManager.isConfigured(),
                "Set BUILDTRACK_DB_PASSWORD to run equipment service integration tests.");

        EquipmentService service = new EquipmentService();
        String uniqueName = "Multimeter-" + UUID.randomUUID();
        Equipment equipment = new Equipment(0, uniqueName, "Measurement", true);

        int id = service.addEquipment(equipment);
        try {
            Equipment found = service.findEquipment(id);
            assertNotNull(found);
            assertEquals(uniqueName, found.getName());
            assertTrue(found.isAvailable());

            service.markEquipmentInUse(id);
            Equipment inUse = service.findEquipment(id);
            assertFalse(inUse.isAvailable());

            service.markEquipmentAvailable(id);
            Equipment available = service.findEquipment(id);
            assertTrue(available.isAvailable());

            List<Equipment> all = service.findAllEquipment();
            assertTrue(all.stream().anyMatch(e -> e.getEquipmentId() == id));

            List<Equipment> availableList = service.findAvailableEquipment();
            assertTrue(availableList.stream().anyMatch(e -> e.getEquipmentId() == id));
        } finally {
            service.deleteEquipment(id);
        }
    }

    @Test
    void throwsWhenChangingAvailabilityOfNonExistentEquipment() {
        EquipmentRepository repository = new EquipmentRepository() {
            @Override
            public Equipment findById(int equipmentId) {
                return null;
            }
        };
        EquipmentService service = new EquipmentService(repository);
        assertThrows(ResourceUnavailableException.class,
                () -> service.setEquipmentAvailability(999999, false));
    }
}

