package org.example.commands;

import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.example.services.TicketService;
import org.example.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeneralCommands}.
 * Verifies general commands like help and role assignment logic.
 */
@ExtendWith(MockitoExtension.class)
public class GeneralCommandsTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private UserService userService;
    @Mock
    private SlashCommandInteractionEvent event;
    @Mock
    private ReplyCallbackAction replyCallbackAction;

    private GeneralCommands generalCommands;

    @BeforeEach
    void setUp() {
        generalCommands = new GeneralCommands(ticketService, userService);
    }

    @Test
    void testHandleHelp() {
        // Arrange
        when(event.replyEmbeds(any(MessageEmbed.class))).thenReturn(replyCallbackAction);

        // Act
        generalCommands.handleHelp(event);

        // Assert
        verify(event).replyEmbeds(any(MessageEmbed.class));
        verify(replyCallbackAction).queue();
    }

    /**
     * Verifies the role assignment logic:
     * 1. Removes all existing managed roles (PM, Project Manager, Developer, QA).
     * 2. Adds the requested new role.
     * 3. Syncs the user's role in the database.
     */
    @Test
    void testHandleSetRole_Success() {
        // Arrange
        OptionMapping roleOption = mock(OptionMapping.class);
        Member member = mock(Member.class);
        Guild guild = mock(Guild.class);
        InteractionHook hook = mock(InteractionHook.class);
        Role newRole = mock(Role.class);
        WebhookMessageCreateAction messageAction = mock(WebhookMessageCreateAction.class);

        when(event.deferReply()).thenReturn(replyCallbackAction);
        when(event.getOption("role")).thenReturn(roleOption);
        when(roleOption.getAsString()).thenReturn("Developer");
        when(event.getMember()).thenReturn(member);
        when(event.getGuild()).thenReturn(guild);
        when(event.getHook()).thenReturn(hook);
        when(member.getIdLong()).thenReturn(123L);
        when(guild.getRolesByName(eq("Developer"), anyBoolean())).thenReturn(java.util.Collections.singletonList(newRole));
        when(hook.sendMessage(anyString())).thenReturn(messageAction);

        // Mock old roles removal
        Role oldRole = mock(Role.class);
        AuditableRestAction<Void> auditableRestAction = mock(AuditableRestAction.class);
        when(member.getRoles()).thenReturn(java.util.Collections.singletonList(oldRole));
        
        // Mocking removals for all possible old roles
        List<String> managedRoles = Arrays.asList("Project Manager", "Developer", "QA", "PM");
        for (String r : managedRoles) {
            if (!r.equals("Developer")) {
                when(guild.getRolesByName(eq(r), anyBoolean())).thenReturn(java.util.Collections.singletonList(oldRole));
            }
        }

        when(guild.removeRoleFromMember(any(Member.class), any(Role.class))).thenReturn(auditableRestAction);
        when(guild.addRoleToMember(any(Member.class), any(Role.class))).thenReturn(auditableRestAction);

        // Act
        generalCommands.handleSetRole(event);

        // Assert
        verify(guild, atLeastOnce()).removeRoleFromMember(eq(member), eq(oldRole));
        verify(guild).addRoleToMember(eq(member), eq(newRole));
        verify(userService).setUserRole(123L, "DEVELOPER");
        verify(messageAction).queue();
    }
}
