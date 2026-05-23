package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class UnresolveCommand implements DiscordCommand {
    private final BackendFacade facade;

    public UnresolveCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        facade.unresolveTicket(event.getChannel().asThreadChannel().getIdLong());
        event.reply("✅ Ticket has been reverted to Claimed.").queue();
    }

    @Override
    public String getName() { return "unresolve"; }

    @Override
    public String getDescription() { return "Revert ticket from pending review"; }
}
