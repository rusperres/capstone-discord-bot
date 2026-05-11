package org.example.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.example.services.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link QACommands}.
 * Verifies QA workflow: reviewing tickets, status updates, and score tracking.
 */
@ExtendWith(MockitoExtension.class)
public class QACommandsTest {

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

    private QACommands qaCommands;

    @BeforeEach
    void setUp() {
        qaCommands = new QACommands(ticketService);
    }

    /**
     * Verifies that marking a ticket as reviewed transitions status and increments QA score.
     */
    @Test
    void testHandleReviewed() {
        // Arrange
        MessageChannelUnion messageChannelUnion = mock(MessageChannelUnion.class);
        when(event.getChannelType()).thenReturn(net.dv8tion.jda.api.entities.channel.ChannelType.GUILD_PUBLIC_THREAD);
        when(event.getChannel()).thenReturn(messageChannelUnion);
        when(messageChannelUnion.asThreadChannel()).thenReturn(threadChannel);
        when(threadChannel.getName()).thenReturn("[PENDING-REVIEW] Test Ticket");
        when(threadChannel.getManager()).thenReturn(threadChannelManager);
        when(threadChannelManager.setName(anyString())).thenReturn(threadChannelManager);
        when(event.getMember()).thenReturn(member);
        when(member.getIdLong()).thenReturn(789L);
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        // Act
        qaCommands.handleReviewed(event);

        // Assert
        verify(threadChannelManager).setName(eq("[REVIEWED] Test Ticket"));
        verify(ticketService).updateThreadStatus(anyLong(), eq("REVIEWED"));
        verify(ticketService).incrementQaScore(789L);
        verify(replyCallbackAction).queue();
    }

    /**
     * Verifies that reversing a review transitions status back to PENDING-REVIEW and decrements QA score.
     */
    @Test
    void testHandleUnreview() {
        // Arrange
        MessageChannelUnion messageChannelUnion = mock(MessageChannelUnion.class);
        when(event.getChannelType()).thenReturn(net.dv8tion.jda.api.entities.channel.ChannelType.GUILD_PUBLIC_THREAD);
        when(event.getChannel()).thenReturn(messageChannelUnion);
        when(messageChannelUnion.asThreadChannel()).thenReturn(threadChannel);
        when(threadChannel.getName()).thenReturn("[REVIEWED] Test Ticket");
        when(threadChannel.getManager()).thenReturn(threadChannelManager);
        when(threadChannelManager.setName(anyString())).thenReturn(threadChannelManager);
        when(event.getMember()).thenReturn(member);
        when(member.getIdLong()).thenReturn(789L);
        when(event.reply(anyString())).thenReturn(replyCallbackAction);

        // Act
        qaCommands.handleUnreview(event);

        // Assert
        verify(threadChannelManager).setName(eq("[PENDING-REVIEW] Test Ticket"));
        verify(ticketService).updateThreadStatus(anyLong(), eq("PENDING-REVIEW"));
        verify(ticketService).decrementQaScore(789L);
        verify(replyCallbackAction).queue();
    }
}
