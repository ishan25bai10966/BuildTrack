package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComponentRepository {

    // CREATE
    public int save(Component component) throws SQLException {

        String sql = """
                INSERT INTO component
                (name, category, unit, unit_cost)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, component.getName());
            statement.setString(2, component.getCategory());
            statement.setString(3, component.getUnit());
            statement.setBigDecimal(4, component.getUnitCost());

            int componentId = getGeneratedId(statement);
            component.setComponentId(componentId);
            return componentId;
        }
    }

    // READ - get all components
    public List<Component> findAll() throws SQLException {

        List<Component> components = new ArrayList<>();

        String sql = """
                SELECT component_id, name, category, unit, unit_cost
                FROM component
                ORDER BY component_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                components.add(createComponentFromResult(resultSet));
            }
        }

        return components;
    }

    // READ - find component by ID
    public Component findById(int componentId) throws SQLException {

        String sql = """
                SELECT component_id, name, category, unit, unit_cost
                FROM component
                WHERE component_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, componentId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createComponentFromResult(resultSet);
                }
            }
        }

        return null;
    }

    // UPDATE
    public void update(Component component) throws SQLException {

        String sql = """
                UPDATE component
                SET name = ?,
                    category = ?,
                    unit = ?,
                    unit_cost = ?
                WHERE component_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, component.getName());
            statement.setString(2, component.getCategory());
            statement.setString(3, component.getUnit());
            statement.setBigDecimal(4, component.getUnitCost());
            statement.setInt(5, component.getComponentId());

            requireAffectedRow(statement.executeUpdate(), component.getComponentId());
        }
    }

    // DELETE
    public void delete(int componentId) throws SQLException {

        String sql = "DELETE FROM component WHERE component_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, componentId);

            requireAffectedRow(statement.executeUpdate(), componentId);
        }
    }

    // Convert database row into Component object
    private Component createComponentFromResult(ResultSet resultSet)
            throws SQLException {

        Component component = new Component();

        component.setComponentId(
                resultSet.getInt("component_id")
        );

        component.setName(
                resultSet.getString("name")
        );

        component.setCategory(
                resultSet.getString("category")
        );

        component.setUnit(
                resultSet.getString("unit")
        );

        component.setUnitCost(resultSet.getBigDecimal("unit_cost"));

        return component;
    }

    private int getGeneratedId(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Could not save component.");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return an ID for the saved component.");
    }

    private void requireAffectedRow(int rows, int componentId) throws SQLException {
        if (rows != 1) {
            throw new SQLException("No component exists with ID " + componentId + ".");
        }
    }
}
