package org.example.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import net.dv8tion.jda.api.JDA;
import org.example.commands.GeneralCommands;
import org.example.database.Classes.LeaderboardEntry;
import org.example.database.Classes.User;
import org.example.database.TicketRepository;
import org.example.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private JDA jda;

    @Mock
    private GeneralCommands generalCommands;

    @Mock
    private HttpExchange exchange;

    private UserController userController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userController = new UserController(123L, jda, userService, ticketRepository, generalCommands);
    }

    @Test
    public void testHandleProfile() throws IOException {
        User user = new User("123", "JDoe", "Dev", 5, 2);
        when(ticketRepository.getUser(123L)).thenReturn(user);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/profile?id=123"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertEquals("application/json", responseHeaders.getFirst("Content-Type"));
    }

    @Test
    public void testHandleMembers() throws IOException {
        LeaderboardEntry entry = new LeaderboardEntry(123L, 5);
        List<LeaderboardEntry> leaderboard = Collections.singletonList(entry);
        when(userService.getLeaderboard("dev")).thenReturn(leaderboard);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/members?type=dev"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    public void testHandleUpdateRole() throws IOException {
        String json = "{\"role\":\"QA\"}";
        when(exchange.getRequestMethod()).thenReturn("PATCH");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/user/123"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        userController.handle(exchange);

        verify(userService).setUserRole(123L, "QA");
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }
}
