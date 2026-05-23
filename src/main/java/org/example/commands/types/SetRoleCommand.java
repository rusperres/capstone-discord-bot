package org.example.commands.types;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.example.commands.DiscordCommand;
import org.example.services.BackendFacade;

public class SetRoleCommand implements DiscordCommand {
    private final BackendFacade facade;

    public SetRoleCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping roleOption = event.getOption("role");
        if (roleOption == null) {
            event.reply("❌ Role is required.").setEphemeral(true).queue();
            return;
        }
        String role = roleOption.getAsString();
        facade.setUserRole(event.getUser().getIdLong(), role);
        event.reply("✅ Your role has been updated to: " + role).queue();
    }

    @Override
    public String getName() {
        return "set-role";
    }

    @Override
    public String getDescription() {
        return "Assign your role";
    }
}
