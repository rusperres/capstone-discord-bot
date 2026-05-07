package org.example.services;

import org.example.database.Classes.LeaderboardEntry;
import org.example.database.Classes.User;
import org.example.database.TicketRepository;

import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private final TicketRepository ticketRepository;

    public UserService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void setUserRole(long userId, String role) {
        ticketRepository.upsertUser(userId, role);
    }

    public List<LeaderboardEntry> getLeaderboard(String type) {
        // TicketRepository returns List<User>, we need to convert to List<LeaderboardEntry>
        List<User> topUsers = ticketRepository.getLeaderboard(type, 10);
        return topUsers.stream()
                .map(user -> new LeaderboardEntry(
                        Long.parseLong(user.getUserId()),
                        type.equalsIgnoreCase("dev") ? user.getDevScore() : user.getQaScore()
                ))
                .collect(Collectors.toList());
    }
}
