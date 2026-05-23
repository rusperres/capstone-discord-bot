package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.AdminCommands;
import org.example.commands.DiscordCommand;

public class SetRemindersCommand implements DiscordCommand {
    private final AdminCommands adminCommands;

    public SetRemindersCommand(AdminCommands adminCommands) {
        this.adminCommands = adminCommands;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        adminCommands.handleSetRemindersChannel(event);
    }

    @Override
    public String getName() { return "set-reminders"; }

    @Override
    public String getDescription() { return "PM only: Set the reminders channel"; }
}
