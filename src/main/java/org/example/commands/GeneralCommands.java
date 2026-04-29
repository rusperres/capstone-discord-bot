package org.example.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GeneralCommands {
    private final TicketService ticketService;

    public GeneralCommands(TicketService ticketService){
        this.ticketService = ticketService;
    }

    public void handleSetRole(SlashCommandInteractionEvent event){
        event.deferReply().queue();
        String roleName = event.getOption("role").getAsString();
        Member member = event.getMember();

        if(member==null)return;

        List<String> managedRoles = Arrays.asList("PM", "Developer", "QA");
        for(String r: managedRoles){
            event.getGuild().getRolesByName(r, true).stream()
                    .findFirst()
                    .ifPresent(role -> event.getGuild().removeRoleFromMember(member, role).queue());
        }

        Role newRole = (Role) event.getGuild().getRolesByName(roleName, true).stream()
                .findFirst()
                .orElse(null);
        if(newRole != null){
            event.getGuild().addRoleToMember(member, newRole).queue();
            ticketService.setUserRole(member.getIdLong(), roleName);
            event.getHook().sendMessage("✅ Role set to " + roleName).queue();
        } else {
            event.getHook().sendMessage("❌ Role " + roleName + " not found in server.").queue();
        }
    }

    public void handleLeaderboard(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        List<LeaderboardEntry> stats = ticketService.getLeaderboard(type);

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🏆 " + type.toUpperCase() + " Leaderboard")
                .setColor(Color.YELLOW);

        for (int i = 0; i < stats.size(); i++) {
            LeaderboardEntry entry = stats.get(i);
            eb.appendDescription((i + 1) + ". <@" + entry.getUserId() + "> - " + entry.getScore() + " tickets\n");
        }

        event.replyEmbeds(eb.build()).queue();
    }

    public void handleClosed(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ This command can only be used in a ticket thread.").setEphemeral(true).queue();
            return;
        }

        String threadName = event.getChannel().getName();
        String cleanName = threadName.replaceAll("\\[.*?\\]", "").trim();
        String newName = "[CLOSED] " + cleanName;

        event.getChannel().asThreadChannel().getManager().setName(newName).queue();
        ticketService.updateThreadStatus(event.getChannel().getIdLong(), "CLOSED");

        event.reply("🔒 Ticket closed.").queue();
    }

    public void handleHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder workflow = new EmbedBuilder()
                .setTitle("📖 Ticket Workflow Guide")
                .setDescription("1. **PM** loads tickets via `/load-tickets`\n" +
                        "2. **Dev** claims via `/claim` and resolves via `/resolved`\n" +
                        "3. **QA** approves via `/reviewed`\n" +
                        "4. Ticket is marked `/closed`")
                .setColor(Color.CYAN);

        event.replyEmbeds(workflow.build()).queue();
    }
}
