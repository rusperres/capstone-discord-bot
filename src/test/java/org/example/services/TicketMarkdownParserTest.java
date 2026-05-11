package org.example.services;

import org.example.database.Classes.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TicketMarkdownParser}.
 * Verifies that markdown tickets are parsed correctly and missing fields handle defaults gracefully (null safety).
 */
public class TicketMarkdownParserTest {

    private TicketMarkdownParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new TicketMarkdownParser();
    }

    @Test
    void testParse_ValidTicket() throws IOException {
        Path ticketFile = tempDir.resolve("TICK-101.md");
        String content = "---\ntitle: Real Bug\npriority: HIGH\nstatus: CLAIMED\n---\nDetailed description here.";
        Files.writeString(ticketFile, content);

        Ticket ticket = parser.parse(ticketFile);

        assertEquals("Real Bug", ticket.getTitle());
        assertEquals("HIGH", ticket.getPriority());
        assertEquals("CLAIMED", ticket.getStatus());
        assertEquals("Detailed description here.", ticket.getDescription());
    }

    @Test
    void testParse_MissingFields_UsesDefaults() throws IOException {
        Path ticketFile = tempDir.resolve("plain.md");
        String content = "---\n---\nMinimal body.";
        Files.writeString(ticketFile, content);

        Ticket ticket = parser.parse(ticketFile);

        // title should default to filename without .md
        assertEquals("plain", ticket.getTitle());
        // status should default to OPEN
        assertEquals("OPEN", ticket.getStatus());
        // priority should default to LOW
        assertEquals("LOW", ticket.getPriority());
        assertEquals("Minimal body.", ticket.getDescription());
    }

    @Test
    void testParse_EmptyBody() throws IOException {
        Path ticketFile = tempDir.resolve("empty_body.md");
        String content = "---\ntitle: Empty Body\n---\n";
        Files.writeString(ticketFile, content);

        Ticket ticket = parser.parse(ticketFile);

        assertEquals("Empty Body", ticket.getTitle());
        assertEquals("", ticket.getDescription());
        assertNotNull(ticket.getDescription());
    }

    @Test
    void testParse_InvalidFormat_ThrowsException() throws IOException {
        Path ticketFile = tempDir.resolve("bad.md");
        String content = "No markers here";
        Files.writeString(ticketFile, content);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(ticketFile));
    }
}
