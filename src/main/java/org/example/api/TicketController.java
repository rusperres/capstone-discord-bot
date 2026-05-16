package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.database.Classes.Ticket;
import org.example.database.TicketRepository;
import org.example.services.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicketController implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;

    public TicketController(TicketService ticketService, TicketRepository ticketRepository) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        logger.info("Received {} request for {}", method, path);

        try {
            if ("GET".equals(method) && "/api/tickets/list".equals(path)) {
                handleListTickets(exchange);
            } else if ("GET".equals(method) && path.matches("/api/tickets/\\d+")) {
                handleGetTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/\\d+/claim")) {
                handleClaimTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/\\d+/resolve")) {
                handleResolveTicket(exchange, path);
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/\\d+/close")) {
                handleUpdateStatus(exchange, path, "CLOSED");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/\\d+/review")) {
                handleUpdateStatus(exchange, path, "IN_REVIEW");
            } else if ("PATCH".equals(method) && path.matches("/api/tickets/\\d+/demote")) {
                handleUpdateStatus(exchange, path, "OPEN");
            } else {
                logger.warn("Route not found: {} {}", method, path);
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling request for {} {}", method, path, e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleListTickets(HttpExchange exchange) throws IOException {
        List<Ticket> tickets = ticketRepository.getAllActiveTickets();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tickets.size(); i++) {
            json.append(tickets.get(i).toJson());
            if (i < tickets.size() - 1) json.append(",");
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private void handleGetTicket(HttpExchange exchange, String path) throws IOException {
        long id = extractId(path);
        Ticket ticket = ticketRepository.findTicketByThreadId(id);
        if (ticket != null) {
            sendResponse(exchange, 200, ticket.toJson());
        } else {
            sendResponse(exchange, 404, "{\"error\":\"Ticket not found\"}");
        }
    }

    private void handleClaimTicket(HttpExchange exchange, String path) throws IOException {
        long ticketId = extractId(path);
        String body = readBody(exchange);
        long userId = extractLongFromJson(body, "userId");
        if (userId == 0) {
            sendResponse(exchange, 400, "{\"error\":\"userId is required\"}");
            return;
        }
        ticketService.assignDeveloper(ticketId, userId);
        logger.info("Ticket {} claimed by user {}", ticketId, userId);
        sendResponse(exchange, 200, "{\"message\":\"Ticket claimed successfully\"}");
    }

    private void handleResolveTicket(HttpExchange exchange, String path) throws IOException {
        long ticketId = extractId(path);
        String body = readBody(exchange);
        String prUrl = extractStringFromJson(body, "prUrl");
        if (prUrl.isEmpty()) {
            sendResponse(exchange, 400, "{\"error\":\"prUrl is required\"}");
            return;
        }
        ticketService.setPrUrl(ticketId, prUrl);
        logger.info("Ticket {} resolved with PR: {}", ticketId, prUrl);
        sendResponse(exchange, 200, "{\"message\":\"Ticket submitted for review\"}");
    }

    private void handleUpdateStatus(HttpExchange exchange, String path, String status) throws IOException {
        long ticketId = extractId(path);
        ticketService.updateThreadStatus(ticketId, status);
        logger.info("Ticket {} status updated to {}", ticketId, status);
        sendResponse(exchange, 200, "{\"message\":\"Ticket status updated to " + status + "\"}");
    }

    private long extractId(String path) {
        Pattern pattern = Pattern.compile("/api/tickets/(\\d+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
            String result = s.hasNext() ? s.next() : "";
            logger.debug("Request body: {}", result);
            return result;
        }
    }

    private long extractLongFromJson(String json, String key) {
        // Matches both "key": 123 and "key": "123"
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"?(\\d+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }

    private String extractStringFromJson(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
