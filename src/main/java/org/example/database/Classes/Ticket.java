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


    private Ticket(TicketBuilder builder) {
        this.ticketId = builder.ticketId;
        this.title = builder.title;
        this.description = builder.description;
        this.status = builder.status;
        this.priority = builder.priority;
        this.categories = builder.categories;
        this.discordThreadId = builder.discordThreadId;
        this.prUrl = builder.prUrl;
        this.claimedBy = builder.claimedBy;
        this.closedBy = builder.closedBy;
        this.date_added = builder.dateAdded;
        this.date_closed = builder.dateClosed;

        validateStatus();
    }

    public static class TicketBuilder {
        private String ticketId;
        private String title;
        private String description;
        private String status;
        private String priority;
        private List<String> categories;
        private String discordThreadId;
        private String prUrl;
        private String claimedBy;
        private String closedBy;
        private String dateAdded;
        private String dateClosed;

        public TicketBuilder setTicketId(String ticketId) { this.ticketId = ticketId; return this; }
        public TicketBuilder setTitle(String title) { this.title = title; return this; }
        public TicketBuilder setDescription(String description) { this.description = description; return this; }
        public TicketBuilder setStatus(String status) { this.status = status; return this; }
        public TicketBuilder setPriority(String priority) { this.priority = priority; return this; }
        public TicketBuilder setCategories(List<String> categories) { this.categories = categories; return this; }
        public TicketBuilder setDiscordThreadId(String discordThreadId) { this.discordThreadId = discordThreadId; return this; }
        public TicketBuilder setPrUrl(String prUrl) { this.prUrl = prUrl; return this; }
        public TicketBuilder setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; return this; }
        public TicketBuilder setClosedBy(String closedBy) { this.closedBy = closedBy; return this; }
        public TicketBuilder setDateAdded(String dateAdded) { this.dateAdded = dateAdded; return this; }
        public TicketBuilder setDateClosed(String dateClosed) { this.dateClosed = dateClosed; return this; }

        public Ticket build() {
            return new Ticket(this);
        }
    }

    private void validateStatus(){
        switch (this.status){
            case "OPEN" -> {}
            case "CLAIMED" -> {}
            case "PENDING-REVIEW" -> {}
            case "REVIEWED" -> {}
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

    public String toJson() {
        StringBuilder cats = new StringBuilder("[");
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                cats.append("\"").append(escapeJson(categories.get(i))).append("\"");
                if (i < categories.size() - 1) cats.append(",");
            }
        }
        cats.append("]");

        return String.format(
                "{\"ticketId\":\"%s\",\"discordThreadId\":\"%s\",\"title\":\"%s\",\"description\":\"%s\",\"status\":\"%s\",\"prUrl\":%s,\"claimedBy\":%s,\"closedBy\":%s,\"priority\":\"%s\",\"categories\":%s,\"dateAdded\":\"%s\",\"dateClosed\":%s}",
                ticketId, discordThreadId, escapeJson(title), escapeJson(description), status,
                prUrl != null ? "\"" + prUrl + "\"" : "null",
                claimedBy != null ? "\"" + claimedBy + "\"" : "null",
                closedBy != null ? "\"" + closedBy + "\"" : "null",
                priority, cats.toString(), date_added != null ? date_added : "",
                date_closed != null ? "\"" + date_closed + "\"" : "null"
        );
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}