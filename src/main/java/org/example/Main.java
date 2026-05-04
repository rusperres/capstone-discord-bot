package org.example;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.commands.*;
import org.example.database.DatabaseManager;
import org.example.services.TicketLoader;

public class Main
{
    public static void main( String[] args ) {
        BotConfig config = new BotConfig();

        if (!config.isValid()) {
            System.err.println("❌ Critical Error: Invalid configuration in .env file.");
            return;
        }

        DatabaseManager db = new DatabaseManager();
        db.initialize();

        TicketLoader loader = new TicketLoader(config.getTicketsDir());
        GeneralCommands general = new GeneralCommands(db);
        AdminCommands admin = new AdminCommands(db, loader);
        DevCommands dev = new DevCommands(db);
        QACommands qa = new QACommands(db);

        CommandManager commandManager = new CommandManager(
                general,
                admin,
                dev,
                qa,
                config.getTicketsDir()
        );
        try {
            JDA jda = JDABuilder.createDefault(config.getDiscordToken())
                    .enableIntents(GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(commandManager)
                    .build();

            jda.awaitReady();
            commandManager.registerCommands(jda);

            System.out.println("🚀 Bot is online in Guild: " + config.getGuildId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
