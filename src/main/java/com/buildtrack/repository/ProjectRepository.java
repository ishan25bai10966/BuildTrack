package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.HardwareProject;
import com.buildtrack.model.IoTProject;
import com.buildtrack.model.MechanicalProject;
import com.buildtrack.model.Project;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {

    // CREATE
    public int save(Project project) throws SQLException {

        String sql = """
                INSERT INTO project
                (name, description, start_date, deadline, budget, project_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());
            setNullableDate(statement, 3, project.getStartDate());
            setNullableDate(statement, 4, project.getDeadline());
            statement.setBigDecimal(5, project.getBudget());
            statement.setString(6, getProjectType(project));

            int projectId = getGeneratedId(statement, "project");
            project.setProjectId(projectId);
            return projectId;
        }
    }

    // READ - get all projects
    public List<Project> findAll() throws SQLException {

        List<Project> projects = new ArrayList<>();

        String sql = """
                SELECT project_id, name, description,
                       start_date, deadline, budget, project_type
                FROM project
                ORDER BY project_id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Project project = createProjectFromResult(resultSet);

                projects.add(project);
            }
        }

        return projects;
    }

    // READ - find project by ID
    public Project findById(int projectId) throws SQLException {

        String sql = """
                SELECT project_id, name, description,
                       start_date, deadline, budget, project_type
                FROM project
                WHERE project_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return createProjectFromResult(resultSet);
                }
            }
        }

        return null;
    }

    // UPDATE
    public void update(Project project) throws SQLException {

        String sql = """
                UPDATE project
                SET name = ?,
                    description = ?,
                    start_date = ?,
                    deadline = ?,
                    budget = ?,
                    project_type = ?
                WHERE project_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());
            setNullableDate(statement, 3, project.getStartDate());
            setNullableDate(statement, 4, project.getDeadline());
            statement.setBigDecimal(5, project.getBudget());
            statement.setString(6, getProjectType(project));
            statement.setInt(7, project.getProjectId());

            requireAffectedRow(statement.executeUpdate(), "project", project.getProjectId());
        }
    }

    // DELETE
    public void delete(int projectId) throws SQLException {

        String sql = "DELETE FROM project WHERE project_id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            requireAffectedRow(statement.executeUpdate(), "project", projectId);
        }
    }

    // Determine the type of project
    private String getProjectType(Project project) {

        if (project instanceof IoTProject) {
            return "IOT";
        }

        if (project instanceof MechanicalProject) {
            return "MECHANICAL";
        }

        if (project instanceof HardwareProject) {
            return "HARDWARE";
        }

        throw new IllegalArgumentException("Unsupported project class: "
                + project.getClass().getName());
    }

    // Convert a database record into the correct Project object
    private Project createProjectFromResult(ResultSet resultSet)
            throws SQLException {

        int projectId = resultSet.getInt("project_id");
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");

        Date startDateValue = resultSet.getDate("start_date");
        Date deadlineValue = resultSet.getDate("deadline");

        LocalDate startDate =
                startDateValue != null ? startDateValue.toLocalDate() : null;

        LocalDate deadline =
                deadlineValue != null ? deadlineValue.toLocalDate() : null;

        java.math.BigDecimal budget = resultSet.getBigDecimal("budget");

        String projectType = resultSet.getString("project_type");

        return switch (projectType) {

            case "IOT" ->
                    new IoTProject(
                            projectId,
                            name,
                            description,
                            startDate,
                            deadline,
                            budget
                    );

            case "MECHANICAL" ->
                    new MechanicalProject(
                            projectId,
                            name,
                            description,
                            startDate,
                            deadline,
                            budget
                    );

            case "HARDWARE" ->
                    new HardwareProject(
                            projectId,
                            name,
                            description,
                            startDate,
                            deadline,
                            budget
                    );

            default -> throw new SQLException("Unknown project_type '"
                    + projectType + "' for project " + projectId);
        };
    }

    private void setNullableDate(PreparedStatement statement, int index,
                                 LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(date));
        }
    }

    private int getGeneratedId(PreparedStatement statement, String entity)
            throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Could not save " + entity + ".");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("Database did not return an ID for the saved "
                + entity + ".");
    }

    private void requireAffectedRow(int rows, String entity, int id)
            throws SQLException {
        if (rows != 1) {
            throw new SQLException("No " + entity + " exists with ID " + id + ".");
        }
    }
}
