package org.example.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.services.TicketService;

public class DevCommands {
    private final TicketService ticketService;

    public DevCommands(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void handleClaim(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ This command must be used inside a ticket thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        Member member = event.getMember();

        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[CLAIMED][" + member.getEffectiveName() + "] " + cleanName;

        thread.getManager().setName(newName).queue();

        ticketService.updateThreadStatus(thread.getIdLong(), "CLAIMED");
        ticketService.assignDeveloper(thread.getIdLong(), member.getIdLong());

        event.reply("🛠️ Ticket claimed by " + member.getAsMention()).queue();
    }

    public void handleUnclaim(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ Not a thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[OPEN] " + cleanName;

        thread.getManager().setName(newName).queue();
        ticketService.updateThreadStatus(thread.getIdLong(), "OPEN");
        ticketService.assignDeveloper(thread.getIdLong(), 0L); // Clear assignment

        event.reply("🔓 Ticket unclaimed and reset to [OPEN].").queue();
    }

    public void handleResolved(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ Not a thread.").setEphemeral(true).queue();
            return;
        }

        String prUrl = event.getOption("pr_url").getAsString();
        ThreadChannel thread = event.getChannel().asThreadChannel();
        Member member = event.getMember();

        // 1. Rename to [PENDING-REVIEW]
        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[PENDING-REVIEW] " + cleanName;

        thread.getManager().setName(newName).queue();

        // 2. Database Update
        ticketService.updateThreadStatus(thread.getIdLong(), "PENDING-REVIEW");
        ticketService.setPrUrl(thread.getIdLong(), prUrl);
        ticketService.incrementDeveloperScore(member.getIdLong());

        event.reply("✅ Ticket resolved! PR: " + prUrl + "\nSubmitting for QA review...").queue();
    }

    public void handleUnresolve(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ Not a thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        Member member = event.getMember();

        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[CLAIMED][" + member.getEffectiveName() + "] " + cleanName;

        thread.getManager().setName(newName).queue();

        ticketService.updateThreadStatus(thread.getIdLong(), "CLAIMED");
        ticketService.decrementDeveloperScore(member.getIdLong());

        event.reply("⚠️ Resolution reverted. Status reset to [CLAIMED].").queue();
    }
}
