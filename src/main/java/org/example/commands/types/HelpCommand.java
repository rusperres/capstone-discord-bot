package org.example.commands.types;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.commands.DiscordCommand;

import java.awt.Color;

public class HelpCommand implements DiscordCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Capybara Issue Tracker - Help Guide");
        embed.setColor(Color.CYAN);
        embed.setDescription("Managed by the OOP Capstone Team");
        embed.addField("General Commands", "`/help` - Show this guide\n`/leaderboard` - Show top contributors\n`/set-role` - Assign your project role", false);
        embed.addField("Developer Commands", "`/claim` - Claim a ticket thread\n`/resolved` - Submit for review (requires PR link)", false);
        embed.addField("QA Commands", "`/reviewed` - Approve a resolved ticket", false);
        event.replyEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() { return "help"; }

    @Override
    public String getDescription() { return "Show help guide"; }
}
