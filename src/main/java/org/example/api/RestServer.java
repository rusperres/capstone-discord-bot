package org.example.api;

import com.sun.net.httpserver.HttpServer;
import org.example.services.BackendFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RestServer {
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private final int port;
    private final BackendFacade facade;
    private HttpServer server;

    public RestServer(int port, BackendFacade facade) {
        this.port = port;
        this.facade = facade;
    }

    public void start() throws IOException {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);
        server = HttpServer.create(address, 0);
        logger.info("REST Server binding to {}", address);
        
        TicketController ticketController = new TicketController(facade);
        UserController userController = new UserController(facade);
        AuthController authController = new AuthController(facade.authService(), facade.userService());

        server.createContext("/api/tickets", ticketController);
        server.createContext("/api/stats", ticketController);
        server.createContext("/api/profile", userController);
        server.createContext("/api/user", userController);
        server.createContext("/api/auth", authController);

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
