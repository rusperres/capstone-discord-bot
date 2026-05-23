package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class ReviewedCommand implements DiscordCommand {
    private final BackendFacade facade;

    public ReviewedCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        facade.approveTicket(event.getChannel().asThreadChannel().getIdLong(), event.getUser().getId());
        event.reply("✅ Ticket approved and reviewed.").queue();
    }

    @Override
    public String getName() {
        return "reviewed";
    }

    @Override
    public String getDescription() {
        return "QA: Approve this ticket";
    }
}
