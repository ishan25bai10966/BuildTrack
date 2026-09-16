package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.BuildStep;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BuildStepRepository {

    // CREATE
    public int save(BuildStep step) throws SQLException {

        String sql = """
                INSERT INTO build_step
                (project_id, name, description, estimated_hours,
                 actual_hours, deadline, status, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, step.getProjectId());
            statement.setString(2, step.getName());
            statement.setString(3, step.getDescription());
            statement.setDouble(4, step.getEstimatedHours());
            statement.setDouble(5, step.getActualHours());

            if (step.getDeadline() != null) {
                statement.setDate(
                        6,
                        Date.valueOf(step.getDeadline())
                );
            } else {
                statement.setNull(6, Types.DATE);
            }

            statement.setString(7, step.getStatus().name());
            statement.setString(8, step.getPriority().name());

            int stepId = getGeneratedId(statement);
            step.setStepId(stepId);
            return stepId;
        }
    }

    // READ - get all steps for a project
    public List<BuildStep> findByProjectId(int projectId)
            throws SQLException {

        List<BuildStep> steps = new ArrayList<>();

        String sql = """
                SELECT step_id, project_id, name, description,
                       estimated_hours, actual_hours,
                       deadline, status, priority
                FROM build_step
                WHERE project_id = ?
                ORDER BY step_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    steps.add(createBuildStepFromResult(resultSet));
                }
            }
        }

        return steps;
    }

    // READ - find one step
    public BuildStep findById(int stepId) throws SQLException {

        String sql = """
                SELECT step_id, project_id, name, description,
                       estimated_hours, actual_hours,
                       deadline, status, priority
                FROM build_step
                WHERE step_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, stepId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createBuildStepFromResult(resultSet);
                }
            }
        }

        return null;
    }

    // UPDATE
    public void update(BuildStep step) throws SQLException {

        String sql = """
                UPDATE build_step
                SET name = ?,
                    description = ?,
                    estimated_hours = ?,
                    actual_hours = ?,
                    deadline = ?,
                    status = ?,
                    priority = ?
                WHERE step_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, step.getName());
            statement.setString(2, step.getDescription());
            statement.setDouble(3, step.getEstimatedHours());
            statement.setDouble(4, step.getActualHours());

            if (step.getDeadline() != null) {
                statement.setDate(
                        5,
                        Date.valueOf(step.getDeadline())
                );
            } else {
                statement.setNull(5, Types.DATE);
            }

            statement.setString(6, step.getStatus().name());
            statement.setString(7, step.getPriority().name());
            statement.setInt(8, step.getStepId());

            requireAffectedRow(statement.executeUpdate(), step.getStepId());
        }
    }

    // DELETE
    public void delete(int stepId) throws SQLException {

        String sql = "DELETE FROM build_step WHERE step_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, stepId);

            requireAffectedRow(statement.executeUpdate(), stepId);
        }
    }

    // Convert database row into BuildStep object
    private BuildStep createBuildStepFromResult(ResultSet resultSet)
            throws SQLException {

        BuildStep step = new BuildStep();

        step.setStepId(resultSet.getInt("step_id"));
        step.setProjectId(resultSet.getInt("project_id"));
        step.setName(resultSet.getString("name"));
        step.setDescription(resultSet.getString("description"));
        step.setEstimatedHours(
                resultSet.getDouble("estimated_hours")
        );
        step.setActualHours(
                resultSet.getDouble("actual_hours")
        );

        Date deadline = resultSet.getDate("deadline");

        if (deadline != null) {
            step.setDeadline(deadline.toLocalDate());
        }

        step.setStatus(
                BuildStep.Status.valueOf(
                        resultSet.getString("status")
                )
        );

        step.setPriority(
                BuildStep.Priority.valueOf(
                        resultSet.getString("priority")
                )
        );

        return step;
    }

    private int getGeneratedId(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Could not save build step.");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return an ID for the saved build step.");
    }

    private void requireAffectedRow(int rows, int stepId) throws SQLException {
        if (rows != 1) {
            throw new SQLException("No build step exists with ID " + stepId + ".");
        }
    }
}
