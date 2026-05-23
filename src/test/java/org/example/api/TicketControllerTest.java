package org.example.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import net.dv8tion.jda.api.JDA;
import org.example.database.Classes.Ticket;
import org.example.services.AuthService;
import org.example.services.BackendFacade;
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

public class TicketControllerTest {

    @Mock
    private BackendFacade facade;

    @Mock
    private JDA jda;

    @Mock
    private HttpExchange exchange;

    private TicketController ticketController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketController = new TicketController(facade);

        // Stub jda to avoid NPE in resolveThread; returning null channel means DB-only path is used
        when(facade.jda()).thenReturn(jda);
        when(jda.getThreadChannelById(anyLong())).thenReturn(null);

        // Mock a valid session for all tests
        Headers requestHeaders = new Headers();
        requestHeaders.add("Cookie", "sessionId=valid_session");
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        AuthService.UserSession mockSession = new AuthService.UserSession("123", "did", "username", "av", "token");
        when(facade.getSession("valid_session")).thenReturn(mockSession);
    }

    @Test
    public void testHandleListTickets() throws IOException {
        Ticket ticket = new Ticket.TicketBuilder()
                .setTicketId("1")
                .setDiscordThreadId("123")
                .setTitle("Title")
                .setDescription("Desc")
                .setStatus("OPEN")
                .setPriority("LOW")
                .setDateAdded("2023-01-01")
                .build();
        List<Ticket> tickets = Collections.singletonList(ticket);
        when(facade.getAllActiveTickets()).thenReturn(tickets);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/tickets/list"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        ticketController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertEquals("application/json", responseHeaders.getFirst("Content-Type"));
    }

    @Test
    public void testHandleGetTicket() throws IOException {
        Ticket ticket = new Ticket.TicketBuilder()
                .setTicketId("1")
                .setDiscordThreadId("123")
                .setTitle("Title")
                .setDescription("Desc")
                .setStatus("OPEN")
                .setPriority("LOW")
                .setDateAdded("2023-01-01")
                .build();
        when(facade.getTicketByThreadId(123L)).thenReturn(ticket);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/tickets/123"));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        ticketController.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    public void testHandleClaimTicket() throws IOException {
        String json = "{\"userId\":\"456\"}";
        when(exchange.getRequestMethod()).thenReturn("PATCH");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/tickets/123/claim"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        ticketController.handle(exchange);

        verify(facade).assignDeveloper(123L, 456L);
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    public void testHandleResolveTicket() throws IOException {
        String json = "{\"prUrl\":\"http://github.com/pr/1\"}";
        when(exchange.getRequestMethod()).thenReturn("PATCH");
        when(exchange.getRequestURI()).thenReturn(URI.create("/api/tickets/123/resolve"));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));
        
        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        ticketController.handle(exchange);

        verify(facade).resolveTicket(123L, "http://github.com/pr/1");
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }
}
