package org.example.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.example.database.Classes.Ticket;
import org.example.database.TicketRepository;
import org.example.services.TicketService;
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
    private TicketService ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private HttpExchange exchange;

    private TicketController ticketController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketController = new TicketController(ticketService, ticketRepository);
    }

    @Test
    public void testHandleListTickets() throws IOException {
        Ticket ticket = new Ticket("1", "123", "Title", "Desc", "OPEN", null, null, null, "LOW", Collections.emptyList(), "2023-01-01", null);
        List<Ticket> tickets = Collections.singletonList(ticket);
        when(ticketRepository.getAllActiveTickets()).thenReturn(tickets);
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
        Ticket ticket = new Ticket("1", "123", "Title", "Desc", "OPEN", null, null, null, "LOW", Collections.emptyList(), "2023-01-01", null);
        when(ticketRepository.findTicketByThreadId(123L)).thenReturn(ticket);
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

        verify(ticketService).assignDeveloper(123L, 456L);
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

        verify(ticketService).setPrUrl(123L, "http://github.com/pr/1");
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }
}
