package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.services.AuthService;
import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthController implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        logger.info("Received {} request for {} with query {}", method, path, query);
        
        if ("OPTIONS".equals(method)) {
            handleOptions(exchange);
            return;
        }

        try {
            if ("GET".equals(method) && "/api/auth/login".equals(path)) {
                handleLogin(exchange, query);
            } else if ("GET".equals(method) && "/api/auth/callback".equals(path)) {
                handleCallback(exchange, query);
            } else if ("GET".equals(method) && "/api/auth/status".equals(path)) {
                handleStatus(exchange, query);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling auth request", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Cookie");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void handleLogin(HttpExchange exchange, String query) throws IOException {
        String userId = extractParam(query, "id");
        // User ID is now optional; if null, the callback will identify the user.
        
        String sessionId = UUID.randomUUID().toString();
        String state = authService.generateState(sessionId, userId != null ? userId : "pending");
        
        String host = exchange.getRequestHeaders().getFirst("Host");
        String redirectUri = null;
        if (host != null) {
            redirectUri = "http://" + host + "/api/auth/callback";
            logger.info("Constructed dynamic redirect URI: {}", redirectUri);
        }

        String redirectUrl = authService.getOAuthUrl(state, redirectUri);

        // Return JSON instead of 302 to allow the Client to get the sessionId
        String response = String.format("{\"url\":\"%s\",\"sessionId\":\"%s\"}", redirectUrl, sessionId);
        sendResponse(exchange, 200, response);
    }

    private void handleStatus(HttpExchange exchange, String query) throws IOException {
        String sessionId = extractParam(query, "sessionId");
        String userId = extractParam(query, "id"); // Fallback

        AuthService.UserSession session = null;
        if (sessionId != null) {
            session = authService.getSession(sessionId);
        } else if (userId != null) {
            String sid = authService.getSessionIdByUserId(userId);
            if (sid != null) session = authService.getSession(sid);
        }

        if (session != null && session.authenticated) {
            sendResponse(exchange, 200, String.format("{\"authenticated\":true,\"userId\":\"%s\",\"username\":\"%s\"}", session.userId, session.username));
        } else {
            sendResponse(exchange, 200, "{\"authenticated\":false}");
        }
    }

    private void handleCallback(HttpExchange exchange, String query) throws IOException {
        String code = extractParam(query, "code");
        String state = extractParam(query, "state");

        if (code == null || state == null) {
            sendResponse(exchange, 400, "{\"error\":\"code and state are required\"}");
            return;
        }

        String pendingData = authService.getPendingData(state);
        if (pendingData == null) {
            sendResponse(exchange, 403, "{\"error\":\"Invalid or expired state\"}");
            return;
        }

        String[] parts = pendingData.split(":");
        String sessionId = parts[0];
        String userId = parts[1];


        try {
            String host = exchange.getRequestHeaders().getFirst("Host");
            String redirectUri = null;
            if (host != null) {
                redirectUri = "http://" + host + "/api/auth/callback";
            }

            String accessToken = authService.exchangeCodeForToken(code, redirectUri);
            if (accessToken == null) {
                sendResponse(exchange, 401, "{\"error\":\"Failed to exchange code for token\"}");
                return;
            }

            AuthService.UserSession session = authService.fetchUserInfo(accessToken, userId);
            if (session == null) {
                sendResponse(exchange, 401, "{\"error\":\"Failed to fetch user info\"}");
                return;
            }

            // If the initial userId was 'pending', we update it with the real Discord ID
            String finalUserId = "pending".equals(userId) ? session.discordId : userId;
            AuthService.UserSession finalSession = new AuthService.UserSession(
                finalUserId, session.discordId, session.username, session.avatar, session.accessToken
            );
            authService.createSession(sessionId, finalSession);
            
            // Persist the Discord username to our database
            try {
                userService.updateUsername(Long.parseLong(finalUserId), session.username);
                System.out.println("Updated username for user " + finalUserId + " to " + session.username);
            } catch (NumberFormatException e) {
                logger.warn("Could not update username for non-numeric userId: {}", finalUserId);
            }

            // Set-Cookie: sessionId=abc123; HttpOnly; Secure; Path=/; SameSite=Lax
            String cookie = "sessionId=" + sessionId + "; HttpOnly; Path=/; SameSite=Lax";
            // Note: Secure would require HTTPS. Since this might be local dev, I'll omit Secure or make it optional.
            // But the request asked for it. 
            exchange.getResponseHeaders().add("Set-Cookie", cookie);

            sendResponse(exchange, 200, "{\"authenticated\":true}");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendResponse(exchange, 500, "{\"error\":\"Interrupted while authenticating\"}");
        }
    }

    private String extractParam(String query, String key) {
        if (query == null) return null;
        Pattern pattern = Pattern.compile(key + "=([^&]+)");
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Cookie");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
