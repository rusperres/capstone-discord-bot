package org.example;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.commands.*;
import org.example.database.DatabaseManager;
import org.example.database.TicketRepository;
import org.example.services.TicketLoader;
import org.example.services.TicketService;
import org.example.services.UserService;


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

        TicketRepository repository = new TicketRepository(db.getConnection());
        TicketLoader loader = new TicketLoader(config.getTicketsDir());
        
        TicketService ticketService = new TicketService(repository);
        UserService userService = new UserService(repository);

        GeneralCommands general = new GeneralCommands(ticketService, userService);
        AdminCommands admin = new AdminCommands(ticketService, loader);
        DevCommands dev = new DevCommands(ticketService);
        QACommands qa = new QACommands(ticketService);

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
            commandManager.registerCommands(jda, config.getGuildId());

            System.out.println("🚀 Bot is online! Connected to " + jda.getGuilds().size() + " guild(s).");
            System.out.println("📌 Configured Guild ID: " + config.getGuildId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
