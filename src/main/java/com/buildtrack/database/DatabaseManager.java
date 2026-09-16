package com.buildtrack.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/buildtrack";
    private static final String DEFAULT_USER = "postgres";

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("BUILDTRACK_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new SQLException("Database password is not configured. "
                    + "Set BUILDTRACK_DB_PASSWORD before connecting.");
        }

        return DriverManager.getConnection(getUrl(), getUser(), password);
    }

    public static boolean isConfigured() {
        String password = System.getenv("BUILDTRACK_DB_PASSWORD");
        return password != null && !password.isBlank();
    }

    private static String getUrl() {
        return getEnvironmentOrDefault("BUILDTRACK_DB_URL", DEFAULT_URL);
    }

    private static String getUser() {
        return getEnvironmentOrDefault("BUILDTRACK_DB_USER", DEFAULT_USER);
    }

    private static String getEnvironmentOrDefault(String name,
                                                   String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
