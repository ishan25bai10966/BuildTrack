package com.buildtrack.service;

import com.buildtrack.exceptions.InvalidProjectException;
import com.buildtrack.exceptions.ResourceUnavailableException;
import com.buildtrack.model.Equipment;
import com.buildtrack.repository.EquipmentRepository;

import java.sql.SQLException;
import java.util.List;

public class EquipmentService {
    private final EquipmentRepository equipmentRepository;

    public EquipmentService() {
        this(new EquipmentRepository());
    }

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public int addEquipment(Equipment equipment) throws SQLException {
        validateEquipment(equipment);
        return equipmentRepository.save(equipment);
    }

    public void updateEquipment(Equipment equipment) throws SQLException {
        validateEquipment(equipment);
        equipmentRepository.update(equipment);
    }

    public Equipment findEquipment(int equipmentId) throws SQLException {
        return equipmentRepository.findById(equipmentId);
    }

    public List<Equipment> findAllEquipment() throws SQLException {
        return equipmentRepository.findAll();
    }

    public List<Equipment> findAvailableEquipment() throws SQLException {
        return equipmentRepository.findAll().stream()
                .filter(Equipment::isAvailable)
                .toList();
    }

    public void deleteEquipment(int equipmentId) throws SQLException {
        equipmentRepository.delete(equipmentId);
    }

    public void setEquipmentAvailability(int equipmentId, boolean available) throws SQLException {
        Equipment equipment = equipmentRepository.findById(equipmentId);
        if (equipment == null) {
            throw new ResourceUnavailableException("Equipment with ID " + equipmentId + " does not exist.");
        }
        equipment.setAvailable(available);
        equipmentRepository.update(equipment);
    }

    public void markEquipmentInUse(int equipmentId) throws SQLException {
        setEquipmentAvailability(equipmentId, false);
    }

    public void markEquipmentAvailable(int equipmentId) throws SQLException {
        setEquipmentAvailability(equipmentId, true);
    }

    public void validateEquipment(Equipment equipment) {
        if (equipment == null) {
            throw new InvalidProjectException("Equipment is required.");
        }
        if (equipment.getName() == null || equipment.getName().isBlank()) {
            throw new InvalidProjectException("Equipment name cannot be blank.");
        }
        if (equipment.getCategory() == null || equipment.getCategory().isBlank()) {
            throw new InvalidProjectException("Equipment category cannot be blank.");
        }
    }
}

