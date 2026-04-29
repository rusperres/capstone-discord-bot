package org.example.commands;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.example.services.TicketLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommands {
    private final TicketService ticketService;
    private final TicketLoader ticketLoader;

    public AdminCommands(TicketService ticketService, TicketLoader ticketLoader) {
        this.ticketService = ticketService;
        this.ticketLoader = ticketLoader;
    }

    public void handleLoadTickets(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String folderName = event.getOption("folder").getAsString();

        try {
            // 1. Ask service for the files
            List<Path> files = ticketLoader.getMarkdownFiles(folderName);

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (ticketService.isTicketLoaded(fileName)) continue;

                // 2. Ask service for the content
                String content = ticketLoader.loadFileContent(file);
                String title = fileName.replace(".md", "");

                event.getChannel().asTextChannel().createThreadChannel("[OPEN] " + title)
                        .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                        .queue(thread -> {
                            List<String> sections = buildSectionMessages(content);
                            for (String section : sections) {
                                thread.sendMessage(section).queue();
                            }
                            ticketService.addThread(thread.getIdLong(), title, "OPEN");
                            ticketService.markTicketLoaded(fileName);
                        });
            }
            event.getHook().sendMessage("✅ Tickets loaded from " + folderName).queue();
        } catch (IOException e) {
            event.getHook().sendMessage("❌ Error: " + e.getMessage()).queue();
        }
    }


    public void handleRebuildDb(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        TextChannel channel = event.getChannel().asTextChannel();

        List<ThreadChannel> threads = channel.getThreadChannels();
        for (ThreadChannel thread : threads) {
            String name = thread.getName();
            String status = "OPEN";
            if (name.contains("[CLAIMED]")) status = "CLAIMED";
            else if (name.contains("[PENDING-REVIEW]")) status = "PENDING-REVIEW";
            else if (name.contains("[REVIEWED]")) status = "REVIEWED";
            else if (name.contains("[CLOSED]")) status = "CLOSED";

            String cleanName = name.replaceAll("\\[.*?\\]", "").trim();
            ticketService.addThread(thread.getIdLong(), cleanName, status);
        }

        event.getHook().sendMessage("✅ Database rebuilt from " + threads.size() + " threads.").queue();
    }

    public void handleSetRemindersChannel(SlashCommandInteractionEvent event) {
        long channelId = event.getOption("channel").getAsChannel().getIdLong();
        ticketService.setSetting("reminders_channel", String.valueOf(channelId));
        event.reply("🔔 Reminders channel set to <#" + channelId + ">").queue();
    }

    private List<String> buildSectionMessages(String content) {
        List<String> messages = new ArrayList<>();
        if (content.length() < 2000) {
            messages.add(content);
        } else {
            int start = 0;
            while (start < content.length()) {
                int end = Math.min(start + 1900, content.length());
                messages.add(content.substring(start, end));
                start = end;
            }
        }
        return messages;
    }
}
