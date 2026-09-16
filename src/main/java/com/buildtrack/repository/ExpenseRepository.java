package com.buildtrack.repository;

import com.buildtrack.database.DatabaseManager;
import com.buildtrack.model.Expense;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {

    public int save(Expense expense) throws SQLException {
        String sql = """
                INSERT INTO expense
                (project_id, description, amount, expense_date, category)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, expense.getProjectId());
            statement.setString(2, expense.getDescription());
            statement.setBigDecimal(3, expense.getAmount());
            setNullableDate(statement, 4, expense.getExpenseDate());
            statement.setString(5, expense.getCategory());

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not save expense.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int expenseId = keys.getInt(1);
                    expense.setExpenseId(expenseId);
                    return expenseId;
                }
            }
            throw new SQLException("Database did not return an ID for the saved expense.");
        }
    }

    public List<Expense> findByProjectId(int projectId) throws SQLException {
        String sql = """
                SELECT expense_id, project_id, description, amount, expense_date, category
                FROM expense WHERE project_id = ? ORDER BY expense_id
                """;
        List<Expense> expenses = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    expenses.add(createFromResult(resultSet));
                }
            }
        }
        return expenses;
    }

    public Expense findById(int expenseId) throws SQLException {
        String sql = """
                SELECT expense_id, project_id, description, amount, expense_date, category
                FROM expense WHERE expense_id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, expenseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? createFromResult(resultSet) : null;
            }
        }
    }

    public void update(Expense expense) throws SQLException {
        String sql = """
                UPDATE expense SET project_id = ?, description = ?, amount = ?,
                    expense_date = ?, category = ? WHERE expense_id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, expense.getProjectId());
            statement.setString(2, expense.getDescription());
            statement.setBigDecimal(3, expense.getAmount());
            setNullableDate(statement, 4, expense.getExpenseDate());
            statement.setString(5, expense.getCategory());
            statement.setInt(6, expense.getExpenseId());
            requireAffectedRow(statement.executeUpdate(), expense.getExpenseId());
        }
    }

    public void delete(int expenseId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM expense WHERE expense_id = ?")) {
            statement.setInt(1, expenseId);
            requireAffectedRow(statement.executeUpdate(), expenseId);
        }
    }

    private Expense createFromResult(ResultSet resultSet) throws SQLException {
        Date date = resultSet.getDate("expense_date");
        return new Expense(resultSet.getInt("expense_id"),
                resultSet.getInt("project_id"), resultSet.getString("description"),
                resultSet.getBigDecimal("amount"),
                date == null ? null : date.toLocalDate(),
                resultSet.getString("category"));
    }

    private void setNullableDate(PreparedStatement statement, int index,
                                 java.time.LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(date));
        }
    }

    private void requireAffectedRow(int rows, int expenseId) throws SQLException {
        if (rows != 1) {
            throw new SQLException("No expense exists with ID " + expenseId + ".");
        }
    }
}
