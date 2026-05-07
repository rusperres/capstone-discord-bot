package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:database.db";
    private Connection connection;

    public DatabaseManager() {
        // Connect automatically when the manager is instantiated
        try {
            connection = DriverManager.getConnection(DB_URL);

            // SQLite disables foreign key constraints by default. We must enable them.
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            System.out.println("Connected to SQLite database successfully.");
        } catch (SQLException e) {
            System.err.println("Critical Error: Failed to connect to the database.");
            e.printStackTrace();
        }
    }


    public Connection getConnection() {
        return connection;
    }

    public void initialize() {
        if (connection == null) return;

        // 1. Users & Authentication
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,    
                username TEXT NOT NULL,
                auth_token TEXT UNIQUE
            );
            """;

        // 2. Project Roles
        String createUserRolesTable = """
            CREATE TABLE IF NOT EXISTS user_roles (
                user_id TEXT,
                role_name TEXT CHECK(role_name IN ('PROJECT_MANAGER', 'DEVELOPER', 'QA')),
                PRIMARY KEY (user_id, role_name),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            );
            """;

        // 3. The Unified Kanban Board
        String createTicketsTable = """
            CREATE TABLE IF NOT EXISTS tickets (
                ticket_id TEXT PRIMARY KEY,        -- Example: "TICK-1" or a UUID
                discord_thread_id TEXT UNIQUE,     -- The Discord ID (Starts as NULL)
                title TEXT NOT NULL,
                ticket_description TEXT NOT NULL,
                status TEXT NOT NULL CHECK(status IN ( 'OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'RESOLVED', 'CLOSED')),
                pr_url TEXT,                 
                claimed_by TEXT,             
                closed_by TEXT,              
                FOREIGN KEY (claimed_by) REFERENCES users(user_id) ON DELETE SET NULL,
                FOREIGN KEY (closed_by) REFERENCES users(user_id) ON DELETE SET NULL
            );
            """;

        // 4. Global Leaderboard
        String createLeaderboardTable = """
                CREATE TABLE IF NOT EXISTS leaderboard_scores (
                user_id TEXT,
                score_type TEXT, -- NEW: 'DEV' or 'QA'
                score INTEGER DEFAULT 0,
                PRIMARY KEY (user_id, score_type), -- Composite key
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            );
            """;

        // 5. Loaded Files Tracker
        String createLoadedFilesTable = """
                CREATE TABLE IF NOT EXISTS loaded_files (
                    file_name TEXT PRIMARY KEY
                );
                """;

        // 6. System Settings
        String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createUserRolesTable);
            stmt.execute(createTicketsTable);
            stmt.execute(createLeaderboardTable);
            stmt.execute(createLoadedFilesTable);
            stmt.execute(createSettingsTable);
            System.out.println("Database schema initialized successfully with the unified board layout.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database tables.");
            e.printStackTrace();
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed cleanly.");
            } catch (SQLException e) {
                System.err.println("Failed to close database connection.");
                e.printStackTrace();
            }
        }
    }



}





