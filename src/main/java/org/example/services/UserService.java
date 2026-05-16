package org.example.services;

import org.example.database.Classes.User;
import org.example.database.TicketRepository;

import java.util.List;

public class UserService {
    private final TicketRepository ticketRepository;

    public UserService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void setUserRole(long userId, String role) {
        ticketRepository.upsertUser(userId, role);
    }

    public void updateUsername(long userId, String username) {
        ticketRepository.updateUsername(userId, username);
    }

    public List<User> getLeaderboard(String type) {
        return ticketRepository.getLeaderboard(type, 10);
    }
}
