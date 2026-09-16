package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.InventoryItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository {

    public int save(InventoryItem item) throws SQLException {

        String sql = """
                INSERT INTO inventory_item
                (component_id, quantity_available)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, item.getComponentId());
            statement.setInt(2, item.getQuantityAvailable());

            int inventoryId = getGeneratedId(statement);
            item.setInventoryId(inventoryId);
            return inventoryId;
        }
    }

    public List<InventoryItem> findAll() throws SQLException {

        List<InventoryItem> inventory = new ArrayList<>();

        String sql = """
                SELECT inventory_id, component_id, quantity_available
                FROM inventory_item
                ORDER BY inventory_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                inventory.add(createInventoryItemFromResult(resultSet));
            }
        }

        return inventory;
    }

    public InventoryItem findById(int inventoryId) throws SQLException {

        String sql = """
                SELECT inventory_id, component_id, quantity_available
                FROM inventory_item
                WHERE inventory_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, inventoryId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createInventoryItemFromResult(resultSet);
                }
            }
        }

        return null;
    }

    public void update(InventoryItem item) throws SQLException {

        String sql = """
                UPDATE inventory_item
                SET component_id = ?,
                    quantity_available = ?
                WHERE inventory_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, item.getComponentId());
            statement.setInt(2, item.getQuantityAvailable());
            statement.setInt(3, item.getInventoryId());

            requireAffectedRow(statement.executeUpdate(), item.getInventoryId());
        }
    }

    public void delete(int inventoryId) throws SQLException {

        String sql = "DELETE FROM inventory_item WHERE inventory_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, inventoryId);

            requireAffectedRow(statement.executeUpdate(), inventoryId);
        }
    }

    private InventoryItem createInventoryItemFromResult(
            ResultSet resultSet) throws SQLException {

        InventoryItem item = new InventoryItem();

        item.setInventoryId(resultSet.getInt("inventory_id"));
        item.setComponentId(resultSet.getInt("component_id"));
        item.setQuantityAvailable(
                resultSet.getInt("quantity_available")
        );

        return item;
    }

    private int getGeneratedId(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Could not save inventory item.");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return an ID for the saved inventory item.");
    }

    private void requireAffectedRow(int rows, int inventoryId) throws SQLException {
        if (rows != 1) {
            throw new SQLException("No inventory item exists with ID " + inventoryId + ".");
        }
    }
}
