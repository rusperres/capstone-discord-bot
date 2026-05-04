package org.example.database.Classes;

public class Ticket {
    private String ticketId;
    private String discordThreadId;
    private String title;
    private String description;
    private String status;
    private String prUrl;
    private String claimedBy;
    private String closedBy;

    public Ticket(String ticketId, String discordThreadId, String title, String description, String status, String prUrl, String claimedBy, String closedBy) {
        this.ticketId = ticketId;
        this.discordThreadId = discordThreadId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.prUrl = prUrl;
        this.claimedBy = claimedBy;
        this.closedBy = closedBy;
    }

    // Getters

    public String getTicketId() { return ticketId; }
    public String getDiscordThreadId() { return discordThreadId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPrUrl() { return prUrl; }
    public String getClaimedBy() { return claimedBy; }
    public String getClosedBy() { return closedBy; }

    // Setters
    public void setDiscordThreadId(String discordThreadId) { this.discordThreadId = discordThreadId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
}