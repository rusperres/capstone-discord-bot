package org.example.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.example.commands.types.*;
import org.example.services.BackendFacade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Behavioral Design Pattern: Command
 * Uses a Map of DiscordCommand objects to dispatch slash command events,
 * decoupling the command invocation from its execution logic.
 */
public class CommandManager extends ListenerAdapter {
    private final Map<String, DiscordCommand> commands = new HashMap<>();
    private final AdminCommands adminCommands;
    private final String ticketsDir;

    public CommandManager(GeneralCommands general, AdminCommands admin, DevCommands dev, QACommands qa, String ticketsDir) {
        this.adminCommands = admin;
        this.ticketsDir = ticketsDir;
    }

    /**
     * Register a BackendFacade-backed command set.
     * Call this after JDA is ready so the facade has a live JDA reference.
     */
    public void registerFacadeCommands(BackendFacade facade) {
        register(new SetRoleCommand(facade));
        register(new HelpCommand());
        register(new LeaderboardCommand(facade));
        register(new ClosedCommand(facade));
        register(new ClaimCommand(facade));
        register(new ResolvedCommand(facade));
        register(new ReviewedCommand(facade));
        register(new LoadTicketsCommand(adminCommands));
        register(new RebuildDbCommand(adminCommands));
        register(new UnclaimCommand(facade));
        register(new UnresolveCommand(facade));
        register(new UnreviewCommand(facade));
        // SetReminders stays in AdminCommands via legacy fallback; wire it too:
        register(new SetRemindersCommand(adminCommands));
    }

    void register(DiscordCommand command) {
        commands.put(command.getName(), command);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordCommand command = commands.get(event.getName());
        if (command != null) {
            command.execute(event);
        } else {
            event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("load-tickets") && event.getFocusedOption().getName().equals("folder")) {
            java.io.File file = new java.io.File(ticketsDir);
            String[] directories = file.list((current, name) -> new java.io.File(current, name).isDirectory());

            if (directories != null) {
                List<String> choices = Arrays.stream(directories)
                        .filter(name -> name.startsWith(event.getFocusedOption().getValue()))
                        .limit(25)
                        .collect(Collectors.toList());
                event.replyChoiceStrings(choices).queue();
            }
        }
    }

    public void registerCommands(JDA jda, long guildId) {
        List<net.dv8tion.jda.api.interactions.commands.build.SlashCommandData> slashCommands = Arrays.asList(
                Commands.slash("set-role", "Assign your role")
                        .addOptions(new OptionData(OptionType.STRING, "role", "Select your role").setRequired(true)
                                .addChoice("Project Manager", "PM")
                                .addChoice("Developer", "Developer")
                                .addChoice("QA", "QA")),

                Commands.slash("load-tickets", "PM only: Load tickets from folder")
                        .addOptions(new OptionData(OptionType.STRING, "folder", "Folder name").setAutoComplete(true).setRequired(true)),

                Commands.slash("claim", "Claim this ticket"),

                Commands.slash("unclaim", "Unclaim this ticket"),

                Commands.slash("resolved", "Submit ticket for review")
                        .addOptions(new OptionData(OptionType.STRING, "pr_url", "Pull Request URL").setRequired(true)),

                Commands.slash("unresolve", "Revert ticket from pending review"),

                Commands.slash("reviewed", "QA: Approve this ticket"),

                Commands.slash("unreview", "Revert ticket from reviewed"),

                Commands.slash("leaderboard", "Show top contributors")
                        .addOptions(new OptionData(OptionType.STRING, "type", "developer or qa").setRequired(true)),

                Commands.slash("help", "Show help guide"),

                Commands.slash("closed", "Close this ticket"),

                Commands.slash("rebuild-db", "PM only: Rebuild DB from Discord threads"),

                Commands.slash("set-reminders", "PM only: Set the reminders channel")
                        .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Channel to send reminders to").setRequired(true))
        );

        if (guildId != 0L) {
            net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(slashCommands).queue(
                        success -> System.out.println("✅ Guild commands registered for: " + guild.getName()),
                        error -> System.err.println("❌ Failed to register guild commands: " + error.getMessage())
                );
                return;
            }
        }

        jda.updateCommands().addCommands(slashCommands).queue(
                success -> System.out.println("✅ Global commands registered."),
                error -> System.err.println("❌ Failed to register global commands: " + error.getMessage())
        );
    }
}
