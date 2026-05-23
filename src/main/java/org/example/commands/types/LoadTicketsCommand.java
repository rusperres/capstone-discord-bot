package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.AdminCommands;
import org.example.commands.DiscordCommand;

public class LoadTicketsCommand implements DiscordCommand {
    private final AdminCommands adminCommands;

    public LoadTicketsCommand(AdminCommands adminCommands) {
        this.adminCommands = adminCommands;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        adminCommands.handleLoadTickets(event);
    }

    @Override
    public String getName() { return "load-tickets"; }

    @Override
    public String getDescription() { return "PM only: Load tickets from folder"; }
}
