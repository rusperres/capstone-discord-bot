package org.example.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommandManager}.
 * Verifies that slash command interactions are correctly routed via the command map.
 *
 * Behavioral Design Pattern: Command — CommandManager uses a Map<String, DiscordCommand>
 * to dispatch events to the correct handler object, replacing the old switch statement.
 */
@ExtendWith(MockitoExtension.class)
public class CommandManagerTest {

    @Mock private AdminCommands adminCommands;
    @Mock private GeneralCommands generalCommands;
    @Mock private DevCommands devCommands;
    @Mock private QACommands qaCommands;
    @Mock private SlashCommandInteractionEvent event;
    @Mock private ReplyCallbackAction replyAction;

    private CommandManager commandManager;

    @BeforeEach
    void setUp() {
        commandManager = new CommandManager(generalCommands, adminCommands, devCommands, qaCommands, "tickets");
    }

    /**
     * Verifies that a registered DiscordCommand is executed when its name is dispatched.
     */
    @Test
    void testRegisteredCommandIsExecuted() {
        // Arrange: create a mock command and register it
        DiscordCommand mockCommand = mock(DiscordCommand.class);
        when(mockCommand.getName()).thenReturn("test-command");
        commandManager.register(mockCommand);

        when(event.getName()).thenReturn("test-command");

        // Act
        commandManager.onSlashCommandInteraction(event);

        // Assert: the command's execute method was called
        verify(mockCommand).execute(event);
    }

    /**
     * Verifies that an unknown command name returns an ephemeral error reply.
     */
    @Test
    void testUnknownCommandRepliesEphemeral() {
        // Arrange
        when(event.getName()).thenReturn("unknown-command");
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(true)).thenReturn(replyAction);

        // Act
        commandManager.onSlashCommandInteraction(event);

        // Assert
        verify(event).reply("Unknown command");
        verify(replyAction).setEphemeral(true);
        verify(replyAction).queue();
    }
}
