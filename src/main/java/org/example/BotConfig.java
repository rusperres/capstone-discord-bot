package org.example;

import io.github.cdimascio.dotenv.Dotenv;

public class BotConfig {
    private final String discordToken;
    private final String ticketsDir;
    private final long guildId;

    public BotConfig() {
        Dotenv dotenv = Dotenv.load();

        this.discordToken = dotenv.get("DISCORD_TOKEN");
        this.ticketsDir = dotenv.get("TICKETS_DIR", "tickets/");

        String guildIdStr = dotenv.get("GUILD_ID");
        this.guildId = (guildIdStr != null) ? Long.parseLong(guildIdStr) : 0L;
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
    public boolean isValid() {
        return discordToken != null && !discordToken.isEmpty();
    }
}
