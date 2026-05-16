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

        String deleteOldRolesSql = "DELETE FROM user_roles WHERE user_id = ?";
        String insertRoleSql = "INSERT INTO user_roles (user_id, role_name) VALUES (?, ?)";

        try (PreparedStatement pstmtUser = connection.prepareStatement(insertUserSql);
             PreparedStatement pstmtDelete = connection.prepareStatement(deleteOldRolesSql);
             PreparedStatement pstmtInsert = connection.prepareStatement(insertRoleSql)) {

            // 1. Insert user if missing
            pstmtUser.setString(1, stringId);
            pstmtUser.executeUpdate();

            // 2. Remove any old roles to enforce 1 role per user constraint despite composite PK
            pstmtDelete.setString(1, stringId);
            pstmtDelete.executeUpdate();

            // 3. Assign new role
            pstmtInsert.setString(1, stringId);
            pstmtInsert.setString(2, role);
            return pstmtInsert.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUsername(long userId, String username) {
        String sql = "INSERT INTO users (user_id, username) VALUES (?, ?) " +
                     "ON CONFLICT(user_id) DO UPDATE SET username = excluded.username";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(userId));
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
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
     * Deletes all tickets and related data for a full rebuild.
     */
    public boolean deleteAllTickets() {
        try {
            connection.createStatement().executeUpdate("DELETE FROM ticket_category_map");
            connection.createStatement().executeUpdate("DELETE FROM loaded_files");
            connection.createStatement().executeUpdate("DELETE FROM tickets");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds a ticket by its exact title (case-insensitive). Returns the first match.
     */
    public Ticket findTicketByTitle(String title) {
        String sql = "SELECT * FROM tickets WHERE lower(title) = lower(?) LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToTicket(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates the description and discord_thread_id of an existing ticket (enrichment after rebuild).
     */
    public boolean updateTicketDescription(String ticketId, String description, String discordThreadId) {
        String sql = "UPDATE tickets SET ticket_description = ?, discord_thread_id = ? WHERE ticket_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, description);
            pstmt.setString(2, discordThreadId);
            pstmt.setString(3, ticketId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

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
            pstmt.setString(10, ticket.getClaimedBy() != null && !ticket.getClaimedBy().trim().isEmpty() ? ticket.getClaimedBy() : null);
            pstmt.setString(11, ticket.getClosedBy() != null && !ticket.getClosedBy().trim().isEmpty() ? ticket.getClosedBy() : null);

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

    /**
     * Changes a ticket's state using the internal ticket UUID.
     */
    public boolean updateTicketStatusByTicketId(String ticketId, String status) {
        String sql = "UPDATE tickets SET status = ? WHERE ticket_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status.toUpperCase());
            pstmt.setString(2, ticketId);
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
     * Assigns a developer to a ticket using the internal ticket UUID.
     */
    public boolean assignDeveloperByTicketId(String ticketId, long userId) {
        String sql = "UPDATE tickets SET claimed_by = ? WHERE ticket_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId == 0 ? null : String.valueOf(userId));
            pstmt.setString(2, ticketId);
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
     * Stores the GitHub/GitLab link for a resolved ticket using the internal ticket UUID.
     */
    public boolean setPrUrlByTicketId(String ticketId, String url) {
        String sql = "UPDATE tickets SET pr_url = ?, status = 'IN_REVIEW' WHERE ticket_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, url);
            pstmt.setString(2, ticketId);
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
     * Retrieves a full ticket row as a Ticket object using the internal ticket UUID.
     */
    public Ticket findTicketByTicketId(String ticketId) {
        String sql = "SELECT * FROM tickets WHERE ticket_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ticketId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        // Speculatively insert to avoid race conditions across multiple worker threads
        String insertSql = "INSERT OR IGNORE INTO ticket_category (category_id, category_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, java.util.UUID.randomUUID().toString());
            pstmt.setString(2, categoryName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Must exist now, just retrieve its ID
        String checkSql = "SELECT category_id FROM ticket_category WHERE category_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("category_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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