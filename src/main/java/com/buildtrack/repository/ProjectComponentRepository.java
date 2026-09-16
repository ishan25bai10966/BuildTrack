package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.ProjectComponent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectComponentRepository {

    public void save(ProjectComponent projectComponent) throws SQLException {

        String sql = """
                INSERT INTO project_component
                (project_id, component_id, quantity_required)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectComponent.getProjectId());
            statement.setInt(2, projectComponent.getComponentId());
            statement.setInt(3, projectComponent.getQuantityRequired());

            requireAffectedRow(statement.executeUpdate(), projectComponent);
        }
    }

    public List<ProjectComponent> findByProjectId(int projectId)
            throws SQLException {

        List<ProjectComponent> components = new ArrayList<>();

        String sql = """
                SELECT project_id, component_id, quantity_required
                FROM project_component
                WHERE project_id = ?
                ORDER BY component_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    components.add(createFromResult(resultSet));
                }
            }
        }

        return components;
    }

    public ProjectComponent findById(int projectId, int componentId)
            throws SQLException {

        String sql = """
                SELECT project_id, component_id, quantity_required
                FROM project_component
                WHERE project_id = ?
                  AND component_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);
            statement.setInt(2, componentId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createFromResult(resultSet);
                }
            }
        }

        return null;
    }

    public void update(ProjectComponent projectComponent)
            throws SQLException {

        String sql = """
                UPDATE project_component
                SET quantity_required = ?
                WHERE project_id = ?
                  AND component_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectComponent.getQuantityRequired());
            statement.setInt(2, projectComponent.getProjectId());
            statement.setInt(3, projectComponent.getComponentId());

            requireAffectedRow(statement.executeUpdate(), projectComponent);
        }
    }

    public void delete(int projectId, int componentId)
            throws SQLException {

        String sql = """
                DELETE FROM project_component
                WHERE project_id = ?
                  AND component_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);
            statement.setInt(2, componentId);

            requireAffectedRow(statement.executeUpdate(), projectId, componentId);
        }
    }

    private ProjectComponent createFromResult(ResultSet resultSet)
            throws SQLException {

        return new ProjectComponent(
                resultSet.getInt("project_id"),
                resultSet.getInt("component_id"),
                resultSet.getInt("quantity_required")
        );
    }

    private void requireAffectedRow(int rows, ProjectComponent projectComponent)
            throws SQLException {
        requireAffectedRow(rows, projectComponent.getProjectId(),
                projectComponent.getComponentId());
    }

    private void requireAffectedRow(int rows, int projectId, int componentId)
            throws SQLException {
        if (rows != 1) {
            throw new SQLException("No project-component link exists for project "
                    + projectId + " and component " + componentId + ".");
        }
    }
}
