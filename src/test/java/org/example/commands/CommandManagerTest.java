package org.example.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommandManager}.
 * Verifies that slash command interactions are correctly routed to respective command handlers.
 */
@ExtendWith(MockitoExtension.class)
public class CommandManagerTest {

    @Mock
    private GeneralCommands generalCommands;
    @Mock
    private AdminCommands adminCommands;
    @Mock
    private DevCommands devCommands;
    @Mock
    private QACommands qaCommands;
    @Mock
    private SlashCommandInteractionEvent event;

    private CommandManager commandManager;

    @BeforeEach
    void setUp() {
        commandManager = new CommandManager(generalCommands, adminCommands, devCommands, qaCommands, "tickets");
    }

    /**
     * Verifies that the "help" command is routed to GeneralCommands.
     */
    @Test
    void testCommandRouting_Help() {
        // Arrange
        when(event.getName()).thenReturn("help");

        // Act
        commandManager.onSlashCommandInteraction(event);

        // Assert
        verify(generalCommands).handleHelp(event);
    }

    /**
     * Verifies that the "claim" command is routed to DevCommands.
     */
    @Test
    void testCommandRouting_Claim() {
        // Arrange
        when(event.getName()).thenReturn("claim");

        // Act
        commandManager.onSlashCommandInteraction(event);

        // Assert
        verify(devCommands).handleClaim(event);
    }
}
