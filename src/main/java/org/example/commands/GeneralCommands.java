package org.example.commands;

import org.example.database.Classes.LeaderboardEntry;
import org.example.services.TicketService;
import org.example.services.UserService;

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
    private final UserService userService;

    public GeneralCommands(TicketService ticketService, UserService userService){
        this.ticketService = ticketService;
        this.userService = userService;
    }

    public void handleSetRole(SlashCommandInteractionEvent event){
        event.deferReply().queue();
        String roleInput = event.getOption("role").getAsString();
        Member member = event.getMember();

        if(member==null)return;

        // Configuration for roles: Shorthand -> {Discord Name, DB Name}
        java.util.Map<String, String[]> roleMap = new java.util.HashMap<>();
        roleMap.put("PM", new String[]{"Project Manager", "PROJECT_MANAGER"});
        roleMap.put("Developer", new String[]{"Developer", "DEVELOPER"});
        roleMap.put("QA", new String[]{"QA", "QA"});

        String[] roles = roleMap.get(roleInput);
        if (roles == null) {
            event.getHook().sendMessage("❌ Invalid role selection.").queue();
            return;
        }

        String discordRoleName = roles[0];
        String dbRoleName = roles[1];

        // Cleanup old roles
        List<String> managedDiscordRoles = Arrays.asList("PM", "Project Manager", "Developer", "QA");
        for(String r: managedDiscordRoles){
            event.getGuild().getRolesByName(r, true).forEach(role -> {
                if (member.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(member, role).queue();
                }
            });
        }

        // Assign new role
        Role newRole = event.getGuild().getRolesByName(discordRoleName, true).stream()
                .findFirst()
                .orElse(null);

        if(newRole != null){
            event.getGuild().addRoleToMember(member, newRole).queue();
            userService.setUserRole(member.getIdLong(), dbRoleName);
            event.getHook().sendMessage("✅ Role set to **" + discordRoleName + "** (Registered as " + dbRoleName + ")").queue();
        } else {
            event.getHook().sendMessage("❌ Discord role **" + discordRoleName + "** not found in server. Please ensure the role exists.").queue();
        }
    }

    public void handleLeaderboard(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        List<org.example.database.Classes.LeaderboardEntry> stats = userService.getLeaderboard(type);

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
