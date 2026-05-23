package org.example.services;

import org.example.database.Classes.Ticket;
import org.example.database.TicketRepository;

import java.util.UUID;

public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public void updateThreadStatus(long threadId, String status) {
        ticketRepository.updateTicketStatus(threadId, status);
    }

    public void updateTicketStatusByTicketId(String ticketId, String status) {
        ticketRepository.updateTicketStatusByTicketId(ticketId, status);
    }

    public void assignDeveloper(long threadId, long userId) {
        ticketRepository.assignDeveloper(threadId, userId);
    }

    public void assignDeveloperByTicketId(String ticketId, long userId) {
        ticketRepository.assignDeveloperByTicketId(ticketId, userId);
    }

    public void setPrUrl(long threadId, String prUrl) {
        ticketRepository.setPrUrl(threadId, prUrl);
    }

    public void setPrUrlByTicketId(String ticketId, String prUrl) {
        ticketRepository.setPrUrlByTicketId(ticketId, prUrl);
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
    public boolean deleteAllTickets() {
        return ticketRepository.deleteAllTickets();
    }

    public Ticket findTicketByTitle(String title) {
        return ticketRepository.findTicketByTitle(title);
    }

    public boolean updateTicketDescription(String ticketId, String description, String discordThreadId) {
        return ticketRepository.updateTicketDescription(ticketId, description, discordThreadId);
    }

// for rebuild
    public void addThread(long threadId, String title, String status) {
        Ticket ticket = new Ticket.TicketBuilder()
                .setTicketId(UUID.randomUUID().toString())
                .setDiscordThreadId(String.valueOf(threadId))
                .setTitle(title)
                .setDescription("")
                .setStatus(status)
                .setPriority("LOW")
                .build();
        ticketRepository.saveTicket(ticket);
    }
// for actual load
    public void addThread(Ticket ticket) {

        ticketRepository.saveTicket(ticket);
    }

    public void setSetting(String key, String value) {
        ticketRepository.saveSetting(key, value);
    }
}
