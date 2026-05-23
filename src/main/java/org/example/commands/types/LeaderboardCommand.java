package org.example.commands.types;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.example.commands.DiscordCommand;
import org.example.database.Classes.User;
import org.example.services.BackendFacade;

import java.awt.Color;
import java.util.List;

public class LeaderboardCommand implements DiscordCommand {
    private final BackendFacade facade;

    public LeaderboardCommand(BackendFacade facade) {
        this.facade = facade;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping typeOption = event.getOption("type");
        String type = typeOption != null ? typeOption.getAsString().toLowerCase() : "developer";
        boolean isQa = type.equals("qa");

        List<User> topUsers = facade.getLeaderboard(type);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 " + type.toUpperCase() + " Leaderboard");
        embed.setColor(Color.ORANGE);

        if (topUsers.isEmpty()) {
            embed.setDescription("No data yet.");
        } else {
            for (int i = 0; i < topUsers.size(); i++) {
                User user = topUsers.get(i);
                int score = isQa ? user.getQaScore() : user.getDevScore();
                embed.addField((i + 1) + ". " + user.getUsername(), score + " tickets", false);
            }
        }

        event.replyEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() { return "leaderboard"; }

    @Override
    public String getDescription() { return "Show top contributors"; }
}
