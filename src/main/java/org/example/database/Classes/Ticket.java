package org.example.database.Classes;


import java.util.List;

public class Ticket {
    private String ticketId;
    private String title;
    private String description;
    private String status;
    private String priority; // new update
    private List<String> categories; // new update

    private String discordThreadId;
    private String prUrl;

    private String claimedBy;

    private String closedBy;
    private String date_closed; // new update
    private String date_added; // new update


    public Ticket(String ticketId, String discordThreadId,
                  String title, String description, String status,
                  String prUrl, String claimedBy, String closedBy,
                  String priority, List<String> categories,
                  String date_added, String date_closed) {
        this.ticketId = ticketId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.categories = categories;

        this.discordThreadId = discordThreadId;
        this.prUrl = prUrl;


        this.status = status;

        this.claimedBy = claimedBy;

        this.closedBy = closedBy;
        this.date_closed = date_closed;
        this.date_added = date_added;

        validateStatus();
    }

    private void validateStatus(){
        switch (this.status){
            case "OPEN" -> {}
            case "IN_PROGRESS" -> {}
            case "IN_REVIEW" -> {}
            case "RESOLVED" -> {}
            case "CLOSED" -> {}
            default -> {
                status = "OPEN";
            }
        }
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
    public String getPriority() {return priority;}
    public List<String> getCategories() {return categories;}
    public String getDate_closed() {return date_closed;}
    public String getDate_added() {return date_added;}

    // Setters
    public void setDiscordThreadId(String discordThreadId) { this.discordThreadId = discordThreadId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }


}