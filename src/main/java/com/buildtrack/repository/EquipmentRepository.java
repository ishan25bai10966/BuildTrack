package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentRepository {

    public int save(Equipment equipment) throws SQLException {

        String sql = """
                INSERT INTO equipment
                (name, category, available)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, equipment.getName());
            statement.setString(2, equipment.getCategory());
            statement.setBoolean(3, equipment.isAvailable());

            int equipmentId = getGeneratedId(statement);
            equipment.setEquipmentId(equipmentId);
            return equipmentId;
        }
    }

    public List<Equipment> findAll() throws SQLException {

        List<Equipment> equipmentList = new ArrayList<>();

        String sql = """
                SELECT equipment_id, name, category, available
                FROM equipment
                ORDER BY equipment_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                equipmentList.add(createEquipmentFromResult(resultSet));
            }
        }

        return equipmentList;
    }

    public Equipment findById(int equipmentId) throws SQLException {

        String sql = """
                SELECT equipment_id, name, category, available
                FROM equipment
                WHERE equipment_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, equipmentId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createEquipmentFromResult(resultSet);
                }
            }
        }

        return null;
    }

    public void update(Equipment equipment) throws SQLException {

        String sql = """
                UPDATE equipment
                SET name = ?,
                    category = ?,
                    available = ?
                WHERE equipment_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, equipment.getName());
            statement.setString(2, equipment.getCategory());
            statement.setBoolean(3, equipment.isAvailable());
            statement.setInt(4, equipment.getEquipmentId());

            requireAffectedRow(statement.executeUpdate(), equipment.getEquipmentId());
        }
    }

    public void delete(int equipmentId) throws SQLException {

        String sql = "DELETE FROM equipment WHERE equipment_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, equipmentId);

            requireAffectedRow(statement.executeUpdate(), equipmentId);
        }
    }

    private Equipment createEquipmentFromResult(
            ResultSet resultSet) throws SQLException {

        Equipment equipment = new Equipment();

        equipment.setEquipmentId(
                resultSet.getInt("equipment_id")
        );

        equipment.setName(
                resultSet.getString("name")
        );

        equipment.setCategory(
                resultSet.getString("category")
        );

        equipment.setAvailable(
                resultSet.getBoolean("available")
        );

        return equipment;
    }

    private int getGeneratedId(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Could not save equipment.");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return an ID for the saved equipment.");
    }

    private void requireAffectedRow(int rows, int equipmentId) throws SQLException {
        if (rows != 1) {
            throw new SQLException("No equipment exists with ID " + equipmentId + ".");
        }
    }
}
