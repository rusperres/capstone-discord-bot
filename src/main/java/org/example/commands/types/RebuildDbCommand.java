package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.AdminCommands;
import org.example.commands.DiscordCommand;

public class RebuildDbCommand implements DiscordCommand {
    private final AdminCommands adminCommands;

    public RebuildDbCommand(AdminCommands adminCommands) {
        this.adminCommands = adminCommands;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        adminCommands.handleRebuildDb(event);
    }

    @Override
    public String getName() { return "rebuild-db"; }

    @Override
    public String getDescription() { return "PM only: Rebuild database from Discord threads"; }
}
