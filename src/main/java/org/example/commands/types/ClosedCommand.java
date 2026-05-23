package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class ClosedCommand implements DiscordCommand {
    private final BackendFacade facade;

    public ClosedCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        facade.closeTicket(event.getChannel().asThreadChannel().getIdLong());
        event.reply("✅ Ticket has been closed.").queue();
    }

    @Override
    public String getName() {
        return "closed";
    }

    @Override
    public String getDescription() {
        return "Close this ticket";
    }
}
