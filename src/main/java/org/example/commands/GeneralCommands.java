package org.example.commands;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.example.services.TicketService;
import org.example.services.UserService;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Member;
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

        performSetRole(event.getGuild(), member, roleInput, msg -> event.getHook().sendMessage(msg).queue());
    }

    public void performSetRole(net.dv8tion.jda.api.entities.Guild guild, Member member, String roleInput, java.util.function.Consumer<String> responseHandler) {
        // Configuration for roles: Shorthand -> {Discord Name, DB Name}
        java.util.Map<String, String[]> roleMap = new java.util.HashMap<>();
        roleMap.put("PM", new String[]{"Project Manager", "PROJECT_MANAGER"});
        roleMap.put("Project Manager", new String[]{"Project Manager", "PROJECT_MANAGER"});
        roleMap.put("Developer", new String[]{"Developer", "DEVELOPER"});
        roleMap.put("QA", new String[]{"QA", "QA"});

        String[] roles = roleMap.get(roleInput);
        if (roles == null) {
            if (responseHandler != null) responseHandler.accept("❌ Invalid role selection.");
            return;
        }

        String discordRoleName = roles[0];
        String dbRoleName = roles[1];

        // Cleanup old roles
        List<String> managedDiscordRoles = Arrays.asList("PM", "Project Manager", "Developer", "QA");
        for(String r: managedDiscordRoles){
            guild.getRolesByName(r, true).forEach(role -> {
                if (member.getRoles().contains(role)) {
                    guild.removeRoleFromMember(member, role).queue();
                }
            });
        }

        // Assign new role
        Role newRole = guild.getRolesByName(discordRoleName, true).stream()
                .findFirst()
                .orElse(null);

        // Always update the database role
        userService.setUserRole(member.getIdLong(), dbRoleName);

        if(newRole != null){
            guild.addRoleToMember(member, newRole).queue();
            if (responseHandler != null) responseHandler.accept("✅ Role set to **" + discordRoleName + "** (Registered as " + dbRoleName + ")");
        } else {
            if (responseHandler != null) responseHandler.accept("⚠️ Registered as " + dbRoleName + " in DB, but Discord role **" + discordRoleName + "** not found in server.");
        }
    }

    public void handleLeaderboard(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        List<org.example.database.Classes.User> stats = userService.getLeaderboard(type);

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🏆 " + type.toUpperCase() + " Leaderboard")
                .setColor(Color.YELLOW);

        for (int i = 0; i < stats.size(); i++) {
            org.example.database.Classes.User entry = stats.get(i);
            int score = type.equalsIgnoreCase("dev") ? entry.getDevScore() : entry.getQaScore();
            eb.appendDescription((i + 1) + ". <@" + entry.getUserId() + "> - " + score + " tickets\n");
        }

        event.replyEmbeds(eb.build()).queue();
    }

    public void handleClosed(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ This command can only be used in a ticket thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        performClosed(thread);
        event.reply("🔒 Ticket closed.").queue();
    }

    public void performClosed(ThreadChannel thread) {
        String threadName = thread.getName();
        String cleanName = threadName.replaceAll("\\[.*?\\]", "").trim();
        String newName = "[CLOSED] " + cleanName;

        thread.getManager().setName(newName).queue();
        ticketService.updateThreadStatus(thread.getIdLong(), "CLOSED");
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
