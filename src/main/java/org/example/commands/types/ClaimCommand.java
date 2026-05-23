package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class ClaimCommand implements DiscordCommand {
    private final BackendFacade facade;

    public ClaimCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Delegate to facade
        facade.assignDeveloper(event.getChannel().asThreadChannel().getIdLong(), event.getUser().getIdLong());
        event.reply("✅ You have claimed this ticket.").queue();
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claim this ticket";
    }
}
