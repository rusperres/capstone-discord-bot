package org.example.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.example.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DevCommands}.
 * Verifies developer workflow: claiming tickets, submitting resolutions, and status tracking.
 */
@ExtendWith(MockitoExtension.class)
public class DevCommandsTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private SlashCommandInteractionEvent event;
    @Mock
    private ThreadChannel threadChannel;
    @Mock
    private ThreadChannelManager threadChannelManager;
    @Mock
    private ReplyCallbackAction replyCallbackAction;
    @Mock
    private Member member;

    private DevCommands devCommands;

    @BeforeEach
    void setUp() {
        devCommands = new DevCommands(ticketService);
    }

    /**
     * Verifies that claiming a ticket updates the thread name and status in the database.
     */
    @Test
    void testHandleClaim() {
        // Arrange
        MessageChannelUnion messageChannelUnion = mock(MessageChannelUnion.class);
        when(event.getChannelType()).thenReturn(net.dv8tion.jda.api.entities.channel.ChannelType.GUILD_PUBLIC_THREAD);
        when(event.getChannel()).thenReturn(messageChannelUnion);
        when(messageChannelUnion.asThreadChannel()).thenReturn(threadChannel);
        when(threadChannel.getName()).thenReturn("[OPEN] Test Ticket");
        when(threadChannel.getManager()).thenReturn(threadChannelManager);
        when(threadChannelManager.setName(anyString())).thenReturn(threadChannelManager);
        when(event.getMember()).thenReturn(member);
        when(member.getEffectiveName()).thenReturn("DevUser");
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        // Act
        devCommands.handleClaim(event);

        // Assert
        verify(threadChannelManager).setName(contains("CLAIMED"));
        verify(threadChannelManager).setName(contains("DevUser"));
        verify(ticketService).updateThreadStatus(anyLong(), eq("CLAIMED"));
        verify(ticketService).assignDeveloper(anyLong(), anyLong());
        verify(replyCallbackAction).queue();
    }

    /**
     * Verifies that resolving a ticket transitions it to PENDING-REVIEW and increments dev score.
     */
    @Test
    void testHandleResolved() {
        // Arrange
        OptionMapping prOption = mock(OptionMapping.class);
        MessageChannelUnion messageChannelUnion = mock(MessageChannelUnion.class);

        when(event.getChannelType()).thenReturn(net.dv8tion.jda.api.entities.channel.ChannelType.GUILD_PUBLIC_THREAD);
        when(event.getChannel()).thenReturn(messageChannelUnion);
        when(messageChannelUnion.asThreadChannel()).thenReturn(threadChannel);
        when(threadChannel.getName()).thenReturn("[CLAIMED][DevUser] Test Ticket");
        when(threadChannel.getManager()).thenReturn(threadChannelManager);
        when(threadChannelManager.setName(anyString())).thenReturn(threadChannelManager);
        when(event.getMember()).thenReturn(member);
        when(member.getIdLong()).thenReturn(456L);
        when(event.getOption("pr_url")).thenReturn(prOption);
        when(prOption.getAsString()).thenReturn("https://github.com/pr/1");
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        // Act
        devCommands.handleResolved(event);

        // Assert
        verify(threadChannelManager).setName(eq("[PENDING-REVIEW] Test Ticket"));
        verify(ticketService).updateThreadStatus(anyLong(), eq("PENDING-REVIEW"));
        verify(ticketService).setPrUrl(anyLong(), eq("https://github.com/pr/1"));
        verify(ticketService).incrementDeveloperScore(456L);
        verify(replyCallbackAction).queue();
    }
}
