package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import org.example.commands.GeneralCommands;
import org.example.database.Classes.LeaderboardEntry;
import org.example.database.Classes.User;
import org.example.database.TicketRepository;
import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserController implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final long guildId;
    private final JDA jda;
    private final UserService userService;
    private final TicketRepository ticketRepository;
    private final GeneralCommands generalCommands;

    public UserController(long guildId, JDA jda, UserService userService, TicketRepository ticketRepository, GeneralCommands generalCommands) {
        this.guildId = guildId;
        this.jda = jda;
        this.userService = userService;
        this.ticketRepository = ticketRepository;
        this.generalCommands = generalCommands;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        logger.info("Received {} request for {} with query {}", method, path, query);

        try {
            if ("GET".equals(method) && "/api/profile".equals(path)) {
                handleProfile(exchange, query);
            } else if ("GET".equals(method) && "/api/user/members".equals(path)) {
                handleMembers(exchange, query);
            } else if ("PATCH".equals(method) && path.matches("/api/user/\\d+")) {
                handleUpdateRole(exchange, path);
            } else {
                logger.warn("Route not found: {} {}", method, path);
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling request for {} {}", method, path, e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleProfile(HttpExchange exchange, String query) throws IOException {
        long userId = extractParam(query, "id");
        if (userId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"id parameter is required\"}");
            return;
        }
        User user = ticketRepository.getUser(userId);
        if (user != null) {
            sendResponse(exchange, 200, user.toJson());
        } else {
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
        }
    }

    private void handleMembers(HttpExchange exchange, String query) throws IOException {
        String type = extractStringParam(query, "type");
        if (type == null || type.isEmpty()) {
            type = "dev"; // default
        }
        List<LeaderboardEntry> leaderboard = userService.getLeaderboard(type);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < leaderboard.size(); i++) {
            LeaderboardEntry entry = leaderboard.get(i);
            json.append(String.format("{\"userId\":\"%d\",\"score\":%d}", entry.getUserId(), entry.getScore()));
            if (i < leaderboard.size() - 1) json.append(",");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleUpdateRole(HttpExchange exchange, String path) throws IOException {
        long userId = extractId(path);
        String body = readBody(exchange);
        String role = extractStringFromJson(body, "role");
        if (role.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"role is required\"}");
            return;
        }

        net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            Member member = guild.getMemberById(userId);
            if (member != null) {
                generalCommands.performSetRole(guild, member, role, msg -> {
                    logger.info("REST API Role update for {}: {}", userId, msg);
                });
                sendResponse(exchange, 200, "{\"message\":\"User role updated successfully (Synced with Discord)\"}");
                return;
            }
        }

        userService.setUserRole(userId, role);
        logger.info("Updated role for user {} to {} (DB only)", userId, role);
        sendResponse(exchange, 200, "{\"message\":\"User role updated successfully (DB only)\"}");
    }

    private long extractId(String path) {
        Pattern pattern = Pattern.compile("/api/user/(\\d+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1;
    }

    private long extractParam(String query, String key) {
        if (query == null) return 0;
        Pattern pattern = Pattern.compile(key + "=(\\d+)");
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }

    private String extractStringParam(String query, String key) {
        if (query == null) return null;
        Pattern pattern = Pattern.compile(key + "=([^&]+)");
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private String extractStringFromJson(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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
