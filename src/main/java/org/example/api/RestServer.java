package org.example.api;

import com.sun.net.httpserver.HttpServer;
import org.example.database.TicketRepository;
import org.example.services.TicketService;
import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RestServer {
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private final int port;
    private final TicketService ticketService;
    private final UserService userService;
    private final TicketRepository ticketRepository;
    private HttpServer server;

    public RestServer(int port, TicketService ticketService, UserService userService, TicketRepository ticketRepository) {
        this.port = port;
        this.ticketService = ticketService;
        this.userService = userService;
        this.ticketRepository = ticketRepository;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        TicketController ticketController = new TicketController(ticketService, ticketRepository);
        UserController userController = new UserController(userService, ticketRepository);

        server.createContext("/api/tickets", ticketController);
        server.createContext("/api/profile", userController);
        server.createContext("/api/user", userController);

        server.setExecutor(null); // creates a default executor
        server.start();
        logger.info("REST Server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("REST Server stopped");
        }
    }
}
