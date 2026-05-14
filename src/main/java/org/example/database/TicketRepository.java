package org.example.database;

import org.example.database.Classes.Ticket;
import org.example.database.Classes.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TicketRepository {
    private final Connection connection;

    public TicketRepository(Connection connection) {
        this.connection = connection;
    }

    /* ---------------------------------------------- USER METHODS -------------------------------------------*/

    public boolean upsertUser(long userId, String role) {
        String stringId = String.valueOf(userId);

        // Ensure the user exists in the main users table first
        String insertUserSql = "INSERT OR IGNORE INTO users (user_id, username) VALUES (?, 'Unknown')";

        // Upsert their specific role
        String upsertRoleSql = """
            INSERT INTO user_roles (user_id, role_name) 
            VALUES (?, ?)
            ON CONFLICT(user_id, role_name) DO UPDATE SET 
            role_name = excluded.role_name;
            """;

        try (PreparedStatement pstmtUser = connection.prepareStatement(insertUserSql);
             PreparedStatement pstmtRole = connection.prepareStatement(upsertRoleSql)) {

            // 1. Insert user if missing
            pstmtUser.setString(1, stringId);
            pstmtUser.executeUpdate();

            // 2. Assign/Update role
            pstmtRole.setString(1, stringId);
            pstmtRole.setString(2, role);
            return pstmtRole.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches a user's role and current points.
     * Returns a customized UserProfile object (defined at the bottom of this file).
     */
    public User getUser(long userId) {
        String stringId = String.valueOf(userId);
        String sql = """
            SELECT u.user_id, u.username, r.role_name, 
                   COALESCE(SUM(CASE WHEN l.score_type = 'DEV' THEN l.score ELSE 0 END), 0) as dev_score,
                   COALESCE(SUM(CASE WHEN l.score_type = 'QA' THEN l.score ELSE 0 END), 0) as qa_score
            FROM users u
            LEFT JOIN user_roles r ON u.user_id = r.user_id
            LEFT JOIN leaderboard_scores l ON u.user_id = l.user_id
            WHERE u.user_id = ?
            GROUP BY u.user_id, u.username, r.role_name;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, stringId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("role_name"),
                        rs.getInt("dev_score"),
                        rs.getInt("qa_score")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // User not found
    }

    public boolean incrementDevScore(long userId) {
        return modifyScore(userId, "DEV", 1);
    }

    public boolean decrementDevScore(long userId) {
        return modifyScore(userId, "DEV", -1);
    }

    public boolean incrementQaScore(long userId) {
        return modifyScore(userId, "QA", 1);
    }

    public boolean decrementQaScore(long userId) {
        return modifyScore(userId, "QA", -1);
    }

    // =========================================
    // 3. LEADERBOARD
    // =========================================

    /**
     * Returns the top N users sorted by their specific score type ('DEV' or 'QA').
     */
    public List<User> getLeaderboard(String type, int limit) {
        List<User> topUsers = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.username, l.score 
            FROM leaderboard_scores l
            JOIN users u ON l.user_id = u.user_id
            WHERE l.score_type = ?
            ORDER BY l.score DESC
            LIMIT ?;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, type.toUpperCase()); // Ensure it matches 'DEV' or 'QA'
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // We create a partial profile just for the leaderboard view
                User profile = new User(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        null, // Role isn't strictly needed for the leaderboard
                        type.equalsIgnoreCase("DEV") ? rs.getInt("score") : 0,
                        type.equalsIgnoreCase("QA") ? rs.getInt("score") : 0
                );
                topUsers.add(profile);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topUsers;
    }
    /* ---------------------------------------------------------------------------------------------------------*/


    /* ---------------------------------------------- TICKET METHODS -------------------------------------------*/

    /**
     * Inserts a new ticket record and automatically processes its categories.
     */
    public boolean saveTicket(Ticket ticket) {
        String sql = """
            INSERT INTO tickets (ticket_id, discord_thread_id, title, ticket_description, 
                                 status, priority, date_added, date_closed, pr_url) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketId());
            pstmt.setString(2, ticket.getDiscordThreadId());
            pstmt.setString(3, ticket.getTitle());
            pstmt.setString(4, ticket.getDescription());

            // Status and Priority safety checks to prevent CHECK constraint crashes
            pstmt.setString(5, ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : "OPEN");
            pstmt.setString(6, ticket.getPriority() != null ? ticket.getPriority().toUpperCase() : "MEDIUM");

            pstmt.setString(7, ticket.getDate_added());
            pstmt.setString(8, ticket.getDate_closed());
            pstmt.setString(9, ticket.getPrUrl());

            // 1. Save the main ticket row
            boolean isSaved = pstmt.executeUpdate() > 0;

            // 2. If the ticket saved successfully, loop through and save the categories!
            if (isSaved && ticket.getCategories() != null) {
                for (String categoryName : ticket.getCategories()) {
                    addCategoryToTicket(ticket.getTicketId(), categoryName);
                }
            }

            return isSaved;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Changes a ticket's state using the Discord thread ID.
     */
    public boolean updateTicketStatus(long threadId, String status) {
        String sql = "UPDATE tickets SET status = ? WHERE discord_thread_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.toUpperCase());
            pstmt.setString(2, String.valueOf(threadId));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean assignDeveloper(long threadId, long userId) {
        String sql = "UPDATE tickets SET claimed_by = ? WHERE discord_thread_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId == 0 ? null : String.valueOf(userId));
            pstmt.setString(2, String.valueOf(threadId));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Stores the GitHub/GitLab link for a resolved ticket.
     */
    public boolean setPrUrl(long threadId, String url) {
        String sql = "UPDATE tickets SET pr_url = ?, status = 'IN_REVIEW' WHERE discord_thread_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, url);
            pstmt.setString(2, String.valueOf(threadId));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Retrieves a full ticket row as a Ticket object using the Discord thread ID.
     */
    public Ticket findTicketByThreadId(long threadId) {
        String sql = "SELECT * FROM tickets WHERE discord_thread_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(threadId));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if not found
    }

    /**
     * Returns a list of all tickets that aren't marked as CLOSED.
     */
    public List<Ticket> getAllActiveTickets() {
        List<Ticket> activeTickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE status != 'CLOSED'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                activeTickets.add(mapResultSetToTicket(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeTickets;
    }

    /**
     * Checks if a specific .md file has already been imported.
     */
    public boolean isFileNameLoaded(String fileName) {
        String sql = "SELECT 1 FROM loaded_files WHERE file_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if a row is found
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markTicketLoaded(String fileName) {
        String sql = "INSERT OR IGNORE INTO loaded_files (file_name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // =========================================
    // CATEGORY MANAGEMENT
    // =========================================

    /**
     * Checks if a category name exists. If it does, returns the ID.
     * If it doesn't, creates a new one and returns the new ID.
     */
    private String getOrCreateCategory(String categoryName) {
        String categoryId = null;

        // 1. Try to find the existing category
        String checkSql = "SELECT category_id FROM ticket_category WHERE category_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                categoryId = rs.getString("category_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. If we found it, return the ID immediately
        if (categoryId != null) {
            return categoryId;
        }

        // 3. If it doesn't exist, generate a new ID and save it!
        categoryId =  java.util.UUID.randomUUID().toString();
        String insertSql = "INSERT INTO ticket_category (category_id, category_name) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, categoryId);
            pstmt.setString(2, categoryName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return categoryId;
    }

    /**
     * Links a category name to a ticket ID in the junction table.
     */
    public boolean addCategoryToTicket(String ticketId, String categoryName) {
        // First, guarantee the category exists and get its ID
        String categoryId = getOrCreateCategory(categoryName);

        // INSERT OR IGNORE prevents a crash if the ticket already has this exact category
        String mapSql = "INSERT OR IGNORE INTO ticket_category_map (ticket_id, category_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(mapSql)) {
            pstmt.setString(1, ticketId);
            pstmt.setString(2, categoryId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // =========================================
    // SETTINGS
    // =========================================

    public void saveSetting(String key, String value) {
        String sql = """
            INSERT INTO settings (key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================
    // HELPER METHODS
    // =========================================

    /**
     * Helper method to map a SQL ResultSet row directly to a Java Ticket object.
     * This prevents you from writing the exact same mapping code multiple times.
     */
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        String currentTicketId = rs.getString("ticket_id");

        // 1. Fetch the list of strings for this specific ticket
        List<String> categoryNames = getCategoryNamesForTicket(currentTicketId);

        return new Ticket(
                rs.getString("ticket_id"),
                rs.getString("discord_thread_id"),
                rs.getString("title"),
                rs.getString("ticket_description"),
                rs.getString("status"),
                rs.getString("pr_url"),
                rs.getString("claimed_by"),
                rs.getString("closed_by"),
                rs.getString("priority"),
                categoryNames,
                rs.getString("date_added"),
                rs.getString("date_closed")

        );
    }




    private List<String> getCategoryNamesForTicket(String ticketId) {
        List<String> categoryNames = new ArrayList<>();
        String sql = """
            SELECT tc.category_name 
            FROM ticket_category_map tcm
            JOIN ticket_category tc ON tcm.category_id = tc.category_id
            WHERE tcm.ticket_id = ?
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categoryNames.add(rs.getString("category_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryNames;
    }


    private boolean modifyScore(long userId, String scoreType, int amount) {
        String sql = """
            INSERT INTO leaderboard_scores (user_id, score_type, score) 
            VALUES (?, ?, ?)
            ON CONFLICT(user_id, score_type) DO UPDATE SET 
            score = score + excluded.score;
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(userId));
            pstmt.setString(2, scoreType); // 'DEV' or 'QA'
            pstmt.setInt(3, amount);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /* ---------------------------------------------------------------------------------------------------------*/












}