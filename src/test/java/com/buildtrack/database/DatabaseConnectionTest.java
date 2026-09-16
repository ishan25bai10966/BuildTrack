package com.buildtrack.database;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    @Test
    void verifiesDatabaseConnectionWhenConfigured() throws Exception {
        Assumptions.assumeTrue(DatabaseManager.isConfigured(),
                "Set BUILDTRACK_DB_PASSWORD to run database connection test.");

        try (Connection connection = DatabaseManager.getConnection()) {
            assertTrue(connection.isValid(2), "Database connection should be valid.");
        }
    }

    @Test
    void verifiesTaskDependencyTableExists() throws Exception {
        Assumptions.assumeTrue(DatabaseManager.isConfigured(),
                "Set BUILDTRACK_DB_PASSWORD to run database schema test.");

        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'task_dependency'
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "task_dependency table must exist in public schema.");
        }
    }
}
