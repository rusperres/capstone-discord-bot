package org.example.api;

import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.api.JDA;
import org.example.commands.DevCommands;
import org.example.commands.GeneralCommands;
import org.example.commands.QACommands;
import org.example.database.TicketRepository;
import org.example.services.AuthService;
import org.example.services.TicketService;
import org.example.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RestServer {
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private final int port;
    private final long guildId;
    private final JDA jda;
    private final TicketService ticketService;
    private final UserService userService;
    private final TicketRepository ticketRepository;
    private final GeneralCommands generalCommands;
    private final DevCommands devCommands;
    private final QACommands qaCommands;
    private final AuthService authService;
    private HttpServer server;

    public RestServer(int port, long guildId, JDA jda, TicketService ticketService, UserService userService, TicketRepository ticketRepository, GeneralCommands general, DevCommands dev, QACommands qa, AuthService authService) {
        this.port = port;
        this.guildId = guildId;
        this.jda = jda;
        this.ticketService = ticketService;
        this.userService = userService;
        this.ticketRepository = ticketRepository;
        this.generalCommands = general;
        this.devCommands = dev;
        this.qaCommands = qa;
        this.authService = authService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        TicketController ticketController = new TicketController(guildId, jda, ticketService, ticketRepository, devCommands, qaCommands, generalCommands, authService);
        UserController userController = new UserController(guildId, jda, userService, ticketRepository, generalCommands, authService);
        AuthController authController = new AuthController(authService, userService);

        server.createContext("/api/tickets", ticketController);
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
