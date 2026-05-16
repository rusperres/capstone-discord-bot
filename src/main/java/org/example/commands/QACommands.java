package org.example.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.services.TicketService;

public class QACommands {
    private final TicketService ticketService;

    public QACommands(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void handleReviewed(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ This command must be used inside a ticket thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        Member member = event.getMember();
        if (member == null) return;

        performReviewed(thread, member);
        event.reply("🧪 Ticket approved by QA: " + member.getAsMention() + ". Great job!").queue();
    }

    public void performReviewed(ThreadChannel thread, Member member) {
        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[REVIEWED] " + cleanName;
        thread.getManager().setName(newName).queue();

        ticketService.updateThreadStatus(thread.getIdLong(), "REVIEWED");
        ticketService.incrementQaScore(member.getIdLong());
    }

    public void handleUnreview(SlashCommandInteractionEvent event) {
        if (!event.getChannelType().isThread()) {
            event.reply("❌ Not a thread.").setEphemeral(true).queue();
            return;
        }

        ThreadChannel thread = event.getChannel().asThreadChannel();
        Member member = event.getMember();
        if (member == null) return;

        performUnreview(thread, member);
        event.reply("⚠️ Review status removed. Ticket set back to [PENDING-REVIEW].").queue();
    }

    public void performUnreview(ThreadChannel thread, Member member) {
        String cleanName = thread.getName().replaceAll("\\[.*?\\]", "").trim();
        String newName = "[PENDING-REVIEW] " + cleanName;
        thread.getManager().setName(newName).queue();

        ticketService.updateThreadStatus(thread.getIdLong(), "PENDING-REVIEW");
        ticketService.decrementQaScore(member.getIdLong());
    }
}
