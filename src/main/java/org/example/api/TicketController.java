package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
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
            } else if ("GET".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+")) {
                handleGetTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/claim")) {
                handleClaimTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/resolve")) {
                handleResolveTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/close")) {
                handleUpdateStatus(exchange, path, "CLOSED");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/review")) {
                handleUpdateStatus(exchange, path, "IN_REVIEW");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/[a-fA-F0-9\\-]+/demote")) {
                handleUpdateStatus(exchange, path, "OPEN");
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

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (ticketService.isTicketLoaded(fileName)) continue;

                Ticket ticket = ticketMarkdownParser.parse(file);
                String title = ticket.getTitle();
                String content = ticket.getDescription();

                channel.createThreadChannel("[OPEN] " + title)
                        .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                        .queue(thread -> {
                            List<String> sections = buildSectionMessages(content);
                            for (String section : sections) {
                                thread.sendMessage(section).queue();
                            }
                            ticketService.addThread(ticket);
                            ticketService.markTicketLoaded(fileName);
                        });
                loadedCount++;
            }
            sendResponse(exchange, 200, "{\"message\":\"Loaded " + loadedCount + " new tickets.\"}");
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

    private void handleClaimTicket(HttpExchange exchange, String path) throws IOException {
        String rawId = extractRawId(path);
        String body = readBody(exchange);
        long userId = extractLongFromJson(body, "userId");
        if (userId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"userId is required\"}");
            return;
        }

        if (isNumeric(rawId)) {
            long ticketId = Long.parseLong(rawId);
            ThreadChannel thread = jda.getThreadChannelById(ticketId);
            if (thread != null) {
                Member member = thread.getGuild().getMemberById(userId);
                if (member != null) {
                    devCommands.performClaim(thread, member);
                    logger.info("Ticket {} claimed by user {} via REST API", ticketId, userId);
                    sendResponse(exchange, 200, "{\"message\":\"Ticket claimed successfully\"}");
                    return;
                }
            }
            // Fallback for numeric ID
            ticketService.assignDeveloper(ticketId, userId);
            ticketService.updateThreadStatus(ticketId, "CLAIMED");
        } else {
            // UUID-based ticket (no Discord thread)
            ticketService.assignDeveloperByTicketId(rawId, userId);
            ticketService.updateTicketStatusByTicketId(rawId, "CLAIMED");
        }
        logger.info("Ticket {} claimed by user {} (DB only)", rawId, userId);
        sendResponse(exchange, 200, "{\"message\":\"Ticket claimed successfully\"}");
    }

    private void handleResolveTicket(HttpExchange exchange, String path) throws IOException {
        String rawId = extractRawId(path);
        String body = readBody(exchange);
        String prUrl = extractStringFromJson(body, "prUrl");
        if (prUrl.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"prUrl is required\"}");
            return;
        }

        if (isNumeric(rawId)) {
            long ticketId = Long.parseLong(rawId);
            ThreadChannel thread = jda.getThreadChannelById(ticketId);
            if (thread != null) {
                Ticket ticket = ticketRepository.findTicketByThreadId(ticketId);
                if (ticket != null && ticket.getClaimedBy() != null) {
                    Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
                    if (member != null) {
                        devCommands.performResolved(thread, member, prUrl);
                        logger.info("Ticket {} resolved with PR {} via REST API", ticketId, prUrl);
                        sendResponse(exchange, 200, "{\"message\":\"Ticket submitted for review\"}");
                        return;
                    }
                }
            }
            ticketService.setPrUrl(ticketId, prUrl);
        } else {
            ticketService.setPrUrlByTicketId(rawId, prUrl);
        }
        logger.info("Ticket {} resolved with PR {} (DB only)", rawId, prUrl);
        sendResponse(exchange, 200, "{\"message\":\"Ticket submitted for review (DB only)\"}");
    }

    private void handleUpdateStatus(HttpExchange exchange, String path, String status) throws IOException {
        String rawId = extractRawId(path);

        if (isNumeric(rawId)) {
            long ticketId = Long.parseLong(rawId);
            ThreadChannel thread = jda.getThreadChannelById(ticketId);
            if (thread != null) {
                switch (status) {
                    case "CLOSED":
                        generalCommands.performClosed(thread);
                        break;
                    case "IN_REVIEW":
                        Ticket ticket = ticketRepository.findTicketByThreadId(ticketId);
                        if (ticket != null && ticket.getClaimedBy() != null) {
                            Member member = thread.getGuild().getMemberById(ticket.getClaimedBy());
                            if (member != null) {
                                qaCommands.performUnreview(thread, member);
                            }
                        }
                        break;
                    case "OPEN":
                        devCommands.performUnclaim(thread);
                        break;
                }
            } else {
                ticketService.updateThreadStatus(ticketId, status);
            }
        } else {
            ticketService.updateTicketStatusByTicketId(rawId, status);
        }

        logger.info("Ticket {} status updated to {} via REST API", rawId, status);
        sendResponse(exchange, 200, "{\"message\":\"Ticket status updated to " + status + "\"}");
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
