package org.example.database;

import org.example.database.Classes.Ticket;
import org.example.database.Classes.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link TicketRepository}.
 * Uses a temporary real SQLite database to verify SQL syntax, constraints, and relationships.
 */
public class TicketRepositoryTest {

    private Connection connection;
    private TicketRepository repository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        Path dbPath = tempDir.resolve("test_database.db");
        String url = "jdbc:sqlite:" + dbPath.toString();
        connection = DriverManager.getConnection(url);

        // Enable foreign keys
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        // Initialize schema (copied from DatabaseManager to ensure exact match)
        initializeSchema();

        repository = new TicketRepository(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void initializeSchema() throws SQLException {
        String[] createTableSqls = {
            "CREATE TABLE users (user_id TEXT PRIMARY KEY, username TEXT NOT NULL, auth_token TEXT UNIQUE);",
            "CREATE TABLE user_roles (user_id TEXT, role_name TEXT CHECK(role_name IN ('PROJECT_MANAGER', 'DEVELOPER', 'QA')), PRIMARY KEY (user_id, role_name), FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE);",
            "CREATE TABLE tickets (ticket_id TEXT PRIMARY KEY, discord_thread_id TEXT UNIQUE, title TEXT NOT NULL, ticket_description TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('OPEN', 'CLAIMED', 'PENDING-REVIEW', 'REVIEWED', 'RESOLVED', 'CLOSED')), priority TEXT NOT NULL CHECK(priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')), date_added DATE, date_closed DATE, pr_url TEXT, claimed_by TEXT, closed_by TEXT, FOREIGN KEY (claimed_by) REFERENCES users(user_id) ON DELETE SET NULL, FOREIGN KEY (closed_by) REFERENCES users(user_id) ON DELETE SET NULL);",
            "CREATE TABLE ticket_category (category_id TEXT PRIMARY KEY, category_name TEXT NOT NULL UNIQUE);",
            "CREATE TABLE ticket_category_map (ticket_id TEXT, category_id TEXT, PRIMARY KEY (ticket_id, category_id), FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE, FOREIGN KEY (category_id) REFERENCES ticket_category(category_id) ON DELETE CASCADE);",
            "CREATE TABLE leaderboard_scores (user_id TEXT, score_type TEXT, score INTEGER DEFAULT 0, PRIMARY KEY (user_id, score_type), FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE);"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTableSqls) {
                stmt.execute(sql);
            }
        }
    }

    @Test
    void testSaveTicket_Success() {
        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(),
                "123456",
                "Fix Bug",
                "Description",
                "OPEN",
                null, null, null, "HIGH",
                Collections.emptyList(),
                null, null
        );

        assertTrue(repository.saveTicket(ticket));
        
        Ticket saved = repository.findTicketByThreadId(123456L);
        assertNotNull(saved);
        assertEquals("Fix Bug", saved.getTitle());
    }

    @Test
    void testUpsertUserAndScore() {
        long userId = 999L;
        assertTrue(repository.upsertUser(userId, "DEVELOPER"));
        
        // Verify user was created
        User user = repository.getUser(userId);
        assertNotNull(user);
        assertEquals("DEVELOPER", user.getRoleName());

        // Test score manipulation
        repository.incrementDevScore(userId);
        user = repository.getUser(userId);
        assertEquals(1, user.getDevScore());
    }

    @Test
    void testForeignKeys_SetNullOnDelete() throws SQLException {
        long userId = 111L;
        repository.upsertUser(userId, "DEVELOPER");

        Ticket ticket = new Ticket(
                "T-01", "111", "User Ticket", "Desc", "CLAIMED",
                null, String.valueOf(userId), null, "LOW",
                Collections.emptyList(),
                null, null
        );
        repository.saveTicket(ticket);

        // Verify claimed_by is set
        Ticket saved = repository.findTicketByThreadId(111L);
        assertEquals(String.valueOf(userId), saved.getClaimedBy());

        // Delete user
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM users WHERE user_id = '111'");
        }

        // Verify claimed_by became NULL (ON DELETE SET NULL)
        saved = repository.findTicketByThreadId(111L);
        assertNull(saved.getClaimedBy());
    }

    @Test
    void testCheckConstraintViolationNormalizedToOpen() {
        Ticket ticket = new Ticket(
                "T-BAD", "222", "Bad Status", "Desc",
                "INVALID_STATUS", // Normalized to OPEN by Ticket.validateStatus()
                null, null, null, "LOW",
                Collections.emptyList(),
                null, null
        );

        // Verification: Domain model should normalize invalid status to OPEN
        assertEquals("OPEN", ticket.getStatus());

        // Repo should successfully save it as OPEN
        assertTrue(repository.saveTicket(ticket));
        
        Ticket saved = repository.findTicketByThreadId(222L);
        assertEquals("OPEN", saved.getStatus());
    }
}
