package org.example.services;

import net.dv8tion.jda.api.JDA;
import org.example.database.Classes.Ticket;
import org.example.database.Classes.User;
import org.example.database.TicketRepository;

import java.util.List;

/**
 * Structural Design Pattern: Facade
 * Provides a simplified interface to a complex set of classes in the backend.
 */
public class BackendFacade {
    private final TicketService ticketService;
    private final UserService userService;
    private final AuthService authService;
    private final TicketRepository ticketRepository;
    private final TicketLoader ticketLoader;
    private final TicketMarkdownParser ticketMarkdownParser;
    private final JDA jda;
    private final long guildId;

    public BackendFacade(TicketService ticketService, UserService userService, AuthService authService,
                         TicketRepository ticketRepository, TicketLoader ticketLoader,
                         TicketMarkdownParser ticketMarkdownParser, JDA jda, long guildId) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.authService = authService;
        this.ticketRepository = ticketRepository;
        this.ticketLoader = ticketLoader;
        this.ticketMarkdownParser = ticketMarkdownParser;
        this.jda = jda;
        this.guildId = guildId;
    }

    // Ticket Operations
    public List<Ticket> getAllActiveTickets() {
        return ticketRepository.getAllActiveTickets();
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.getAllTickets();
    }

    public Ticket getTicketByThreadId(long threadId) {
        return ticketRepository.findTicketByThreadId(threadId);
    }

    public Ticket getTicketByTicketId(String ticketId) {
        return ticketRepository.findTicketByTicketId(ticketId);
    }

    public boolean saveTicket(Ticket ticket) {
        return ticketRepository.saveTicket(ticket);
    }

    public void assignDeveloper(long threadId, long userId) {
        ticketService.assignDeveloper(threadId, userId);
    }

    public void unclaimTicket(long threadId) {
        ticketRepository.updateTicketClaimedBy(threadId, null);
        ticketService.updateThreadStatus(threadId, "OPEN");
    }

    public void resolveTicket(long threadId, String prUrl) {
        ticketService.setPrUrl(threadId, prUrl);
        ticketService.updateThreadStatus(threadId, "PENDING-REVIEW");
    }

    public void approveTicket(long threadId, String reviewerId) {
        ticketRepository.updateTicketClosedBy(threadId, reviewerId);
        ticketService.updateThreadStatus(threadId, "REVIEWED");
    }

    public void closeTicket(long threadId) {
        ticketService.updateThreadStatus(threadId, "CLOSED");
    }

    public void unresolveTicket(long threadId) {
        ticketService.updateThreadStatus(threadId, "CLAIMED");
    }

    public void unreviewTicket(long threadId) {
        ticketService.updateThreadStatus(threadId, "PENDING-REVIEW");
    }

    // User Operations
    public User getUser(long userId) {
        return repository().getUser(userId);
    }

    public List<User> getLeaderboard(String type) {
        return userService.getLeaderboard(type);
    }

    public List<User> getAllMembers() {
        return ticketRepository.getAllUsers();
    }

    public void setUserRole(long userId, String role) {
        userService.setUserRole(userId, role);
    }

    // Auth Operations
    public AuthService.UserSession getSession(String sessionId) {
        return authService.getSession(sessionId);
    }

    // Helper/Internal access for complex operations
    public TicketRepository repository() { return ticketRepository; }
    public JDA jda() { return jda; }
    public long guildId() { return guildId; }
    public TicketLoader loader() { return ticketLoader; }
    public TicketMarkdownParser parser() { return ticketMarkdownParser; }
    public TicketService ticketService() { return ticketService; }
    public AuthService authService() { return authService; }
    public UserService userService() { return userService; }
}
