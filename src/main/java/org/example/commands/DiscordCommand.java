package org.example.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Behavioral Design Pattern: Command
 * Interface for all Discord slash commands.
 */
public interface DiscordCommand {
    void execute(SlashCommandInteractionEvent event);
    String getName();
    String getDescription();
}
