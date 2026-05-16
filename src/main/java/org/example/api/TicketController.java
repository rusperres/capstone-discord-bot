package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.example.commands.DevCommands;
import org.example.commands.GeneralCommands;
import org.example.commands.QACommands;
import org.example.database.Classes.Ticket;
import org.example.database.TicketRepository;
import org.example.services.AuthService;
import org.example.services.TicketLoader;
import org.example.services.TicketMarkdownParser;
import org.example.services.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketController implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final JDA jda;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final DevCommands devCommands;
    private final QACommands qaCommands;
    private final GeneralCommands generalCommands;
    private final AuthService authService;
    private final TicketLoader ticketLoader;
    private final TicketMarkdownParser ticketMarkdownParser;

    public TicketController(long guildId, JDA jda, TicketService ticketService, TicketRepository ticketRepository, DevCommands devCommands, QACommands qaCommands, GeneralCommands generalCommands, AuthService authService, TicketLoader ticketLoader, TicketMarkdownParser ticketMarkdownParser) {
        this.jda = jda;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.devCommands = devCommands;
        this.qaCommands = qaCommands;
        this.generalCommands = generalCommands;
        this.authService = authService;
        this.ticketLoader = ticketLoader;
        this.ticketMarkdownParser = ticketMarkdownParser;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        logger.info("Received {} request for {}", method, path);

        AuthService.UserSession session = validateSession(exchange);
        if (session == null) {
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized - Invalid or missing sessionId\"}");
            return;
        }

        try {
            if ("GET".equals(method) && "/api/tickets/list".equals(path)) {
                handleListTickets(exchange);
            } else if ("POST".equals(method) && "/api/tickets/load".equals(path)) {
                handleLoadTickets(exchange);
            } else if ("POST".equals(method) && "/api/tickets/rebuild".equals(path)) {
                handleRebuildDb(exchange);
            } else if ("POST".equals(method) && "/api/tickets".equals(path)) {
                handleCreateTicket(exchange);
            } else if ("GET".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+")) {
                handleGetTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/claim")) {
                handleClaimTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/resolve")) {
                handleResolveTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/unresolve")) {
                handleUpdateStatus(exchange, path, "UNRESOLVE");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/unreview")) {
                handleUpdateStatus(exchange, path, "UNREVIEW");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/close")) {
                handleUpdateStatus(exchange, path, "CLOSED");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/review")) {
                handleUpdateStatus(exchange, path, "REVIEWED");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/demote")) {
                handleUpdateStatus(exchange, path, "OPEN");
            } else if ("GET".equals(method) && "/api/tickets/folders".equals(path)) {
                handleListFolders(exchange);
            } else {
                logger.warn("Route not found: {} {}", method, path);
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling request for {} {}", method, path, e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleListTickets(HttpExchange exchange) throws IOException {
        List<Ticket> tickets = ticketRepository.getAllActiveTickets();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tickets.size(); i++) {
            json.append(tickets.get(i).toJson());
            if (i < tickets.size() - 1) json.append(",");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleCreateTicket(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String title = extractStringFromJson(body, "title");
        String content = extractStringFromJson(body, "description");
        String priority = extractStringFromJson(body, "priority");
        String status = extractStringFromJson(body, "status");

        if (title.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"title is required\"}");
            return;
        }

        Ticket ticket = new Ticket(
                java.util.UUID.randomUUID().toString(),
                null, // No discord thread yet
                title,
                content,
                status.isEmpty() ? "OPEN" : status,
                null, // prUrl
                null, // claimedBy
                null, // closedBy
                priority.isEmpty() ? "MEDIUM" : priority,
                new ArrayList<>(), // categories
                new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(new java.util.Date()),
                null // dateClosed
        );

        boolean saved = ticketRepository.saveTicket(ticket);
        if (saved) {
            logger.info("Created new manual ticket: {}", ticket.getTicketId());
            sendResponse(exchange, 201, ticket.toJson());
        } else {
            sendResponse(exchange, 500, "{\"error\":\"Failed to save ticket into database\"}");
        }
    }

    private void handleGetTicket(HttpExchange exchange, String path) throws IOException {
        String rawId = extractRawId(path);
        Ticket ticket;
        if (isNumeric(rawId)) {
            ticket = ticketRepository.findTicketByThreadId(Long.parseLong(rawId));
        } else {
            ticket = ticketRepository.findTicketByTicketId(rawId);
        }
        if (ticket != null) {
            sendResponse(exchange, 200, ticket.toJson());
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Ticket not found\"}");
        }
    }

    private void handleLoadTickets(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        String folderName = extractStringFromJson(body, "folder");
        long channelId = extractLongFromJson(body, "channelId");

        if (folderName.isEmpty() || channelId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"folder and channelId are required\"}");
            return;
        }

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid channelId\"}");
            return;
        }

        try {
            List<Path> files = ticketLoader.getMarkdownFiles(folderName);
            int loadedCount = 0;
            int enrichedCount = 0;

            for (Path file : files) {
                String fileName = file.getFileName().toString();

                Ticket ticket = ticketMarkdownParser.parse(file);
                String title = ticket.getTitle();
                String content = ticket.getDescription();

                // Check if a ticket with this title already exists (e.g. from a prior rebuild)
                Ticket existing = ticketService.findTicketByTitle(title);
                if (existing != null) {
                    // Enrich the existing record with description (don't create a new thread)
                    ticketService.updateTicketDescription(existing.getTicketId(), content, existing.getDiscordThreadId());
                    ticketService.markTicketLoaded(fileName);
                    enrichedCount++;
                    continue;
                }

                if (ticketService.isTicketLoaded(fileName)) continue;

                // No existing record — create a new Discord thread
                channel.createThreadChannel("[OPEN] " + title)
                        .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                        .queue(thread -> {
                            List<String> sections = buildSectionMessages(content);
                            for (String section : sections) {
                                thread.sendMessage(section).queue();
                            }
                            ticket.setDiscordThreadId(String.valueOf(thread.getIdLong()));
                            ticketService.addThread(ticket);
                            ticketService.markTicketLoaded(fileName);
                        });
                loadedCount++;
            }
            sendResponse(exchange, 200, "{\"message\":\"Loaded " + loadedCount + " new tickets, enriched " + enrichedCount + " existing.\"}" );
        } catch (IOException e) {
            logger.error("Error loading tickets", e);
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private List<String> buildSectionMessages(String content) {
        List<String> messages = new ArrayList<>();
        if (content.length() < 2000) {
            messages.add(content);
        } else {
            int start = 0;
            while (start < content.length()) {
                int end = Math.min(start + 1900, content.length());
                messages.add(content.substring(start, end));
                start = end;
            }
        }
        return messages;
    }

    private void handleRebuildDb(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        long channelId = extractLongFromJson(body, "channelId");

        if (channelId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"channelId is required\"}");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid channelId\"}");
            return;
        }

        List<ThreadChannel> threads = channel.getThreadChannels();

        // Clear all existing ticket data for a clean rebuild
        ticketService.deleteAllTickets();
        logger.info("Cleared all existing ticket data for rebuild");

        // Deduplicate: keep only the latest thread per unique title
        // (thread list from JDA is ordered oldest-first, so later entries overwrite earlier ones)
        java.util.Map<String, ThreadChannel> latestByTitle = new java.util.LinkedHashMap<>();
        for (ThreadChannel thread : threads) {
            String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
            latestByTitle.put(cleanName.toLowerCase(), thread); // overwrite older duplicates
        }

        int rebuilt = 0;
        for (java.util.Map.Entry<String, ThreadChannel> entry : latestByTitle.entrySet()) {
            ThreadChannel thread = entry.getValue();
            String name = thread.getName();
            String status = "OPEN";
            if (name.contains("[CLAIMED]")) status = "CLAIMED";
            else if (name.contains("[PENDING-REVIEW]")) status = "PENDING-REVIEW";
            else if (name.contains("[REVIEWED]")) status = "REVIEWED";
            else if (name.contains("[CLOSED]")) status = "CLOSED";

            String cleanName = name.replaceAll("\\[.*?\\]", "").trim();
            ticketService.addThread(thread.getIdLong(), cleanName, status);
            rebuilt++;
        }

        logger.info("Database rebuilt from {} unique threads in channel {} ({} total threads scanned)", rebuilt, channelId, threads.size());
        sendResponse(exchange, 200, "{\"message\":\"Database rebuilt from " + rebuilt + " unique tickets (" + threads.size() + " threads scanned).\"}" );
    }

    private void handleClaimTicket(HttpExchange exchange, String path) throws IOException {
        String rawId = extractRawId(path);
        String body = readBody(exchange);
        long userId = extractLongFromJson(body, "userId");
        if (userId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"userId is required\"}");
            return;
        }

        Ticket ticket = findTicket(rawId);
        ThreadChannel thread = resolveThread(ticket, rawId);

        if (thread != null) {
            Member member = thread.getGuild().getMemberById(userId);
            if (member != null) {
                devCommands.performClaim(thread, member);
                logger.info("Ticket {} claimed by user {} via REST API (Discord synced)", rawId, userId);
                sendResponse(exchange, 200, "{\"message\":\"Ticket claimed successfully\"}");
                return;
            }
        }

        // DB-only fallback
        if (isNumeric(rawId)) {
            ticketService.assignDeveloper(Long.parseLong(rawId), userId);
            ticketService.updateThreadStatus(Long.parseLong(rawId), "CLAIMED");
        } else {
            ticketService.assignDeveloperByTicketId(rawId, userId);
            ticketService.updateTicketStatusByTicketId(rawId, "CLAIMED");
        }
        logger.info("Ticket {} claimed by user {} (DB only)", rawId, userId);
        sendResponse(exchange, 200, "{\"message\":\"Ticket claimed successfully (DB only)\"}");
    }

    private void handleResolveTicket(HttpExchange exchange, String path) throws IOException {
        String rawId = extractRawId(path);
        String body = readBody(exchange);
        String prUrl = extractStringFromJson(body, "prUrl");
        if (prUrl.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"prUrl is required\"}");
            return;
        }

        Ticket ticket = findTicket(rawId);
        ThreadChannel thread = resolveThread(ticket, rawId);

        if (thread != null && ticket != null && ticket.getClaimedBy() != null) {
            Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
            if (member != null) {
                devCommands.performResolved(thread, member, prUrl);
                logger.info("Ticket {} resolved with PR {} via REST API (Discord synced)", rawId, prUrl);
                sendResponse(exchange, 200, "{\"message\":\"Ticket submitted for review\"}");
                return;
            }
        }

        // DB-only fallback
        if (isNumeric(rawId)) {
            ticketService.setPrUrl(Long.parseLong(rawId), prUrl);
        } else {
            ticketService.setPrUrlByTicketId(rawId, prUrl);
        }
        logger.info("Ticket {} resolved with PR {} (DB only)", rawId, prUrl);
        sendResponse(exchange, 200, "{\"message\":\"Ticket submitted for review (DB only)\"}");
    }

    private void handleUpdateStatus(HttpExchange exchange, String path, String status) throws IOException {
        String rawId = extractRawId(path);

        Ticket ticket = findTicket(rawId);
        ThreadChannel thread = resolveThread(ticket, rawId);

        if (thread != null) {
            switch (status) {
                case "CLOSED":
                    generalCommands.performClosed(thread);
                    break;
                case "REVIEWED":
                    if (ticket != null && ticket.getClaimedBy() != null) {
                        Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
                        if (member != null) {
                            qaCommands.performReviewed(thread, member);
                        }
                    }
                    break;
                case "OPEN":
                    devCommands.performUnclaim(thread);
                    break;
                case "UNRESOLVE":
                    if (ticket != null && ticket.getClaimedBy() != null) {
                        Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
                        if (member != null) {
                            devCommands.performUnresolve(thread, member);
                        }
                    }
                    break;
                case "UNREVIEW":
                    if (ticket != null && ticket.getClaimedBy() != null) {
                        Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
                        if (member != null) {
                            qaCommands.performUnreview(thread, member);
                        }
                    }
                    break;
            }
            logger.info("Ticket {} status updated to {} via REST API (Discord synced)", rawId, status);
        } else {
            // DB-only fallback
            if (isNumeric(rawId)) {
                ticketService.updateThreadStatus(Long.parseLong(rawId), status);
            } else {
                ticketService.updateTicketStatusByTicketId(rawId, status);
            }
            logger.info("Ticket {} status updated to {} (DB only)", rawId, status);
        }

        sendResponse(exchange, 200, "{\"message\":\"Ticket status updated to " + status + "\"}");
    }

    private void handleListFolders(HttpExchange exchange) throws IOException {
        String ticketsDir = ticketLoader.getTicketsDir();
        java.io.File file = new java.io.File(ticketsDir);
        String[] directories = file.list((current, name) -> new java.io.File(current, name).isDirectory());

        StringBuilder json = new StringBuilder("[");
        if (directories != null) {
            for (int i = 0; i < directories.length; i++) {
                json.append("\"").append(directories[i]).append("\"");
                if (i < directories.length - 1) json.append(",");
            }
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    /**
     * Finds a ticket from the DB using either a numeric thread ID or UUID ticket ID.
     */
    private Ticket findTicket(String rawId) {
        if (isNumeric(rawId)) {
            return ticketRepository.findTicketByThreadId(Long.parseLong(rawId));
        } else {
            return ticketRepository.findTicketByTicketId(rawId);
        }
    }

    /**
     * Resolves the Discord ThreadChannel for a ticket.
     * If rawId is numeric, tries it directly as a thread ID.
     * Otherwise, looks up the ticket's discordThreadId from the DB.
     */
    private ThreadChannel resolveThread(Ticket ticket, String rawId) {
        if (isNumeric(rawId)) {
            return jda.getThreadChannelById(Long.parseLong(rawId));
        }
        // For UUID-based tickets, get the discord_thread_id from the ticket record
        if (ticket != null && ticket.getDiscordThreadId() != null
                && !ticket.getDiscordThreadId().equals("null")
                && !ticket.getDiscordThreadId().isEmpty()) {
            try {
                return jda.getThreadChannelById(Long.parseLong(ticket.getDiscordThreadId()));
            } catch (NumberFormatException e) {
                logger.warn("Invalid discordThreadId '{}' for ticket {}", ticket.getDiscordThreadId(), rawId);
            }
        }
        return null;
    }

    private String extractRawId(String path) {
        Pattern pattern = Pattern.compile("/api/tickets/([a-fA-F0-9\\-]+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            String result = s.hasNext() ? s.next() : "";
            logger.debug("Request body: {}", result);
            return result;
        }
    }

    private long extractLongFromJson(String json, String key) {
        // Matches both "key": 123 and "key": "123"
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"?(\\d+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }

    private String extractStringFromJson(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private AuthService.UserSession validateSession(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                String[] parts = cookie.split(";");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("sessionId=")) {
                        String sessionId = part.substring("sessionId=".length());
                        return authService.getSession(sessionId);
                    }
                }
            }
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
