package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class UnclaimCommand implements DiscordCommand {
    private final BackendFacade facade;

    public UnclaimCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        facade.unclaimTicket(event.getChannel().asThreadChannel().getIdLong());
        event.reply("✅ Ticket has been unclaimed.").queue();
    }

    @Override
    public String getName() { return "unclaim"; }

    @Override
    public String getDescription() { return "Unclaim this ticket"; }
}
