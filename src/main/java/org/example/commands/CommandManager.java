package org.example.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager extends ListenerAdapter {
    private final GeneralCommands generalCommands;
    private final AdminCommands adminCommands;
    private final DevCommands devCommands;
    private final QACommands qaCommands;
    private final String ticketsDir;

    public CommandManager(GeneralCommands general, AdminCommands admin, DevCommands dev, QACommands qa, String ticketsDir) {
        this.generalCommands = general;
        this.adminCommands = admin;
        this.devCommands = dev;
        this.qaCommands = qa;
        this.ticketsDir = ticketsDir;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        switch (commandName) {
            // General / Priority 1
            case "set-role":    generalCommands.handleSetRole(event); break;
            case "help":        generalCommands.handleHelp(event); break;
            case "leaderboard": generalCommands.handleLeaderboard(event); break;
            case "closed":      generalCommands.handleClosed(event); break;

            // Admin / Priority 2
            case "load-tickets": adminCommands.handleLoadTickets(event); break;
            case "rebuild-db":   adminCommands.handleRebuildDb(event); break;
            case "set-reminders": adminCommands.handleSetRemindersChannel(event); break;

            // Dev / Priority 3
            case "claim":       devCommands.handleClaim(event); break;
            case "unclaim":     devCommands.handleUnclaim(event); break;
            case "resolved":    devCommands.handleResolved(event); break;
            case "unresolve":   devCommands.handleUnresolve(event); break;

            // QA / Priority 3
            case "reviewed":    qaCommands.handleReviewed(event); break;
            case "unreview":    qaCommands.handleUnreview(event); break;

            default:
                event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    public void onCommandAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("load-tickets") && event.getFocusedOption().getName().equals("folder")) {
            // Java 8 logic for filtering directory names
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

    public void registerCommands(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("set-role", "Assign your role")
                        .addOptions(new OptionData(OptionType.STRING, "role", "Dev, QA, or PM").setRequired(true)),

                Commands.slash("load-tickets", "PM only: Load tickets from folder")
                        .addOptions(new OptionData(OptionType.STRING, "folder", "Folder name").setAutoComplete(true).setRequired(true)),

                Commands.slash("claim", "Claim this ticket"),

                Commands.slash("resolved", "Submit ticket for review")
                        .addOptions(new OptionData(OptionType.STRING, "pr_url", "Pull Request URL").setRequired(true)),

                Commands.slash("reviewed", "QA: Approve this ticket"),

                Commands.slash("leaderboard", "Show top contributors")
                        .addOptions(new OptionData(OptionType.STRING, "type", "developer or qa").setRequired(true)),

                Commands.slash("help", "Show help guide"),

                Commands.slash("closed", "Close this ticket")
        ).queue();
    }
}
