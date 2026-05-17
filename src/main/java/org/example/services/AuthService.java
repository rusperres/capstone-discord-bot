package org.example.services;

import org.example.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final BotConfig config;
    private final HttpClient httpClient;

    // state -> "sessionId:userId"
    private final Map<String, String> pendingStates = new ConcurrentHashMap<>();
    
    // sessionId -> UserSession
    private final Map<String, UserSession> authenticatedSessions = new ConcurrentHashMap<>();

    public AuthService(BotConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String generateState(String sessionId, String userId) {
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, sessionId + ":" + userId);
        return state;
    }

    public String getPendingData(String state) {
        return pendingStates.remove(state);
    }

    public void createSession(String sessionId, UserSession session) {
        authenticatedSessions.put(sessionId, session);
    }

    public UserSession getSession(String sessionId) {
        return authenticatedSessions.get(sessionId);
    }

    public String getSessionIdByUserId(String userId) {
        for (Map.Entry<String, UserSession> entry : authenticatedSessions.entrySet()) {
            if (entry.getValue().userId.equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String getOAuthUrl(String state, String redirectUri) {
        String uri = (redirectUri != null) ? redirectUri : config.getDiscordRedirectUri();
        return "https://discord.com/api/oauth2/authorize" +
                "?client_id=" + config.getDiscordClientId() +
                "&redirect_uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=identify" +
                "&state=" + state;
    }

    public String exchangeCodeForToken(String code, String redirectUri) throws IOException, InterruptedException {
        String uri = (redirectUri != null) ? redirectUri : config.getDiscordRedirectUri();
        String form = "client_id=" + config.getDiscordClientId() +
                "&client_secret=" + config.getDiscordClientSecret() +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.error("Failed to exchange code: {} {}", response.statusCode(), response.body());
            return null;
        }

        return extractFromJson(response.body(), "access_token");
    }

    public UserSession fetchUserInfo(String accessToken, String userId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/users/@me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.error("Failed to fetch user info: {} {}", response.statusCode(), response.body());
            return null;
        }

        String username = extractFromJson(response.body(), "username");
        String avatar = extractFromJson(response.body(), "avatar");
        String discordId = extractFromJson(response.body(), "id");

        return new UserSession(userId, discordId, username, avatar, accessToken);
    }

    private String extractFromJson(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"?([^\",}]*)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\"", "");
        }
        return null;
    }

    public static class UserSession {
        public final String userId;
        public final String discordId;
        public final String username;
        public final String avatar;
        public final String accessToken;
        public final boolean authenticated;

        public UserSession(String userId, String discordId, String username, String avatar, String accessToken) {
            this.userId = userId;
            this.discordId = discordId;
            this.username = username;
            this.avatar = avatar;
            this.accessToken = accessToken;
            this.authenticated = true;
        }
    }
}
