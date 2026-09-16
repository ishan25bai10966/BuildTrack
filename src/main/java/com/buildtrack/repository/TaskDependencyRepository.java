package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.TaskDependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskDependencyRepository {

    public void save(TaskDependency dependency) throws SQLException {
        String sql = "INSERT INTO task_dependency (step_id, prerequisite_step_id) VALUES (?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dependency.getStepId());
            statement.setInt(2, dependency.getPrerequisiteStepId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not save task dependency.");
            }
        }
    }

    public void delete(int stepId, int prerequisiteStepId) throws SQLException {
        String sql = "DELETE FROM task_dependency WHERE step_id = ? AND prerequisite_step_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            statement.setInt(2, prerequisiteStepId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No task dependency exists for steps " + stepId + " and " + prerequisiteStepId + ".");
            }
        }
    }

    public List<TaskDependency> findByStepId(int stepId) throws SQLException {
        String sql = "SELECT step_id, prerequisite_step_id FROM task_dependency WHERE step_id = ?";
        List<TaskDependency> dependencies = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stepId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    dependencies.add(new TaskDependency(resultSet.getInt("step_id"),
                            resultSet.getInt("prerequisite_step_id")));
                }
            }
        }
        return dependencies;
    }
}
