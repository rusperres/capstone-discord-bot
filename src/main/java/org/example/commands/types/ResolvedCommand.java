package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class ResolvedCommand implements DiscordCommand {
    private final BackendFacade facade;

    public ResolvedCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping prOption = event.getOption("pr_url");
        if (prOption == null) {
            event.reply("❌ PR URL is required.").setEphemeral(true).queue();
            return;
        }
        String prUrl = prOption.getAsString();
        facade.resolveTicket(event.getChannel().asThreadChannel().getIdLong(), prUrl);
        event.reply("✅ Ticket submitted for review: " + prUrl).queue();
    }

    @Override
    public String getName() {
        return "resolved";
    }

    @Override
    public String getDescription() {
        return "Submit ticket for review";
    }
}
