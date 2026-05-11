package org.example.commands;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.example.database.Classes.Ticket;
import org.example.services.TicketLoader;
import org.example.services.TicketMarkdownParser;
import org.example.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminCommands}.
 * Verifies administrative actions like setting reminder channels and loading tickets.
 */
@ExtendWith(MockitoExtension.class)
public class AdminCommandsTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private TicketLoader ticketLoader;
    @Mock
    private TicketMarkdownParser ticketMarkdownParser;
    @Mock
    private SlashCommandInteractionEvent event;
    @Mock
    private ReplyCallbackAction replyCallbackAction;

    private AdminCommands adminCommands;

    @BeforeEach
    void setUp() {
        adminCommands = new AdminCommands(ticketService, ticketLoader, ticketMarkdownParser);
    }

    /**
     * Verifies that /set-reminders correctly updates the ticket service settings.
     */
    @Test
    void testHandleSetRemindersChannel() {
        // Arrange
        OptionMapping channelOption = mock(OptionMapping.class);
        GuildChannelUnion channelUnion = mock(GuildChannelUnion.class);
        long channelId = 123456789L;

        when(event.getOption("channel")).thenReturn(channelOption);
        when(channelOption.getAsChannel()).thenReturn(channelUnion);
        when(channelUnion.getIdLong()).thenReturn(channelId);
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        // Act
        adminCommands.handleSetRemindersChannel(event);

        // Assert
        verify(ticketService).setSetting("reminders_channel", String.valueOf(channelId));
        verify(event).reply(contains(String.valueOf(channelId)));
        verify(replyCallbackAction).queue();
    }

    /**
     * Verifies the full workflow of /load-tickets:
     * 1. Fetches files from loader.
     * 2. Parses markdown into Ticket objects.
     * 3. Creates thread channels in Discord.
     */
    @Test
    void testHandleLoadTickets() throws java.io.IOException {
        // Arrange
        OptionMapping folderOption = mock(OptionMapping.class);
        InteractionHook hook = mock(InteractionHook.class);
        WebhookMessageCreateAction messageAction = mock(WebhookMessageCreateAction.class);
        MessageChannelUnion channelUnion = mock(MessageChannelUnion.class);
        TextChannel textChannel = mock(TextChannel.class);

        when(event.deferReply()).thenReturn(replyCallbackAction);
        when(event.getOption("folder")).thenReturn(folderOption);
        when(folderOption.getAsString()).thenReturn("sprint1");
        when(event.getHook()).thenReturn(hook);
        when(hook.sendMessage(anyString())).thenReturn(messageAction);
        when(event.getChannel()).thenReturn(channelUnion);
        when(channelUnion.asTextChannel()).thenReturn(textChannel);

        java.nio.file.Path mockPath = mock(java.nio.file.Path.class);
        java.nio.file.Path fileNamePath = mock(java.nio.file.Path.class);
        when(mockPath.getFileName()).thenReturn(fileNamePath);
        when(fileNamePath.toString()).thenReturn("ticket1.md");
        when(ticketLoader.getMarkdownFiles("sprint1")).thenReturn(java.util.Collections.singletonList(mockPath));
        when(ticketService.isTicketLoaded("ticket1.md")).thenReturn(false);

        Ticket mockTicket = mock(Ticket.class);
        when(ticketMarkdownParser.parse(mockPath)).thenReturn(mockTicket);
        when(mockTicket.getTitle()).thenReturn("Test Bug");
        when(mockTicket.getDescription()).thenReturn("Some content");

        ThreadChannelAction threadChannelAction = mock(ThreadChannelAction.class);
        when(textChannel.createThreadChannel(anyString())).thenReturn(threadChannelAction);
        when(threadChannelAction.setAutoArchiveDuration(any())).thenReturn(threadChannelAction);

        // Act
        adminCommands.handleLoadTickets(event);

        // Assert
        verify(ticketLoader).getMarkdownFiles("sprint1");
        verify(ticketMarkdownParser).parse(mockPath);
        verify(textChannel).createThreadChannel("[OPEN] Test Bug");
        verify(threadChannelAction).queue(any());
        verify(messageAction).queue();
    }
}
