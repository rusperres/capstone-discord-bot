package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class UnreviewCommand implements DiscordCommand {
    private final BackendFacade facade;

    public UnreviewCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        facade.unreviewTicket(event.getChannel().asThreadChannel().getIdLong());
        event.reply("✅ Ticket has been reverted to Pending Review.").queue();
    }

    @Override
    public String getName() { return "unreview"; }

    @Override
    public String getDescription() { return "Revert ticket from reviewed"; }
}
