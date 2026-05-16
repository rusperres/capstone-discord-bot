package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class BotConfig {
    private final String discordToken;
    private final String ticketsDir;
    private final long guildId;
    private final String discordClientId;
    private final String discordClientSecret;
    private final String discordRedirectUri;

    public BotConfig() {
        Dotenv dotenv = Dotenv.load();

        this.discordToken = dotenv.get("DISCORD_TOKEN");
        this.ticketsDir = dotenv.get("TICKETS_DIR", "tickets/");

        String guildIdStr = dotenv.get("GUILD_ID");
        this.guildId = (guildIdStr != null) ? Long.parseLong(guildIdStr) : 0L;

        this.discordClientId = dotenv.get("DISCORD_CLIENT_ID");
        this.discordClientSecret = dotenv.get("DISCORD_CLIENT_SECRET");
        this.discordRedirectUri = dotenv.get("DISCORD_REDIRECT_URI");
    }
    public String getDiscordToken() {
        return discordToken;
    }
    public String getTicketsDir() {
        return ticketsDir;
    }
    public long getGuildId() {
        return guildId;
    }
    public String getDiscordClientId() {
        return discordClientId;
    }
    public String getDiscordClientSecret() {
        return discordClientSecret;
    }
    public String getDiscordRedirectUri() {
        return discordRedirectUri;
    }
    public boolean isValid() {
        return discordToken != null && !discordToken.isEmpty();
    }
}
