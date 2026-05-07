package org.example.services;

import org.example.database.Classes.Ticket;
import org.example.database.TicketRepository;

public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void updateThreadStatus(long threadId, String status) {
        ticketRepository.updateTicketStatus(threadId, status);
    }

    public void assignDeveloper(long threadId, long userId) {
        ticketRepository.assignDeveloper(threadId, userId);
    }

    public void setPrUrl(long threadId, String prUrl) {
        ticketRepository.setPrUrl(threadId, prUrl);
    }

    public void incrementDeveloperScore(long userId) {
        ticketRepository.incrementDevScore(userId);
    }

    public void decrementDeveloperScore(long userId) {
        ticketRepository.decrementDevScore(userId);
    }

    public void incrementQaScore(long userId) {
        ticketRepository.incrementQaScore(userId);
    }

    public void decrementQaScore(long userId) {
        ticketRepository.decrementQaScore(userId);
    }

    public boolean isTicketLoaded(String fileName) {
        return ticketRepository.isFileNameLoaded(fileName);
    }

    public void markTicketLoaded(String fileName) {
        ticketRepository.markTicketLoaded(fileName);
    }

    public void addThread(long threadId, String title, String status) {
        Ticket ticket = new Ticket(
                "TICK-" + (System.currentTimeMillis() % 10000), // Temporary ID generation
                String.valueOf(threadId),
                title,
                "", // No description initially
                status,
                null, null, null
        );
        ticketRepository.saveTicket(ticket);
    }

    public void setSetting(String key, String value) {
        ticketRepository.saveSetting(key, value);
    }
}
