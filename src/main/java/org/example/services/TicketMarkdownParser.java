package org.example.services;

import org.example.database.Classes.Ticket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TicketMarkdownParser {

    public Ticket parse(Path file) throws IOException {
        String raw = Files.readString(file);

        String[] parts = raw.split("---");

        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid ticket format: missing metadata block");
        }

        String metaBlock = parts[1].trim();
        String body = parts[2].trim();

        Map<String, String> meta = parseMeta(metaBlock);

        String title = meta.getOrDefault("title", file.getFileName().toString().replace(".md", ""));
        String priority = meta.getOrDefault("priority", "LOW");
        String status = meta.getOrDefault("status", "OPEN");
        String prUrl = meta.getOrDefault("prUrl", null);
        String claimedBy = meta.getOrDefault("claimedBy", null);
        String closedBy = meta.getOrDefault("closedBy", null);
        String dateAdded = meta.getOrDefault("dateAdded", null);
        String dateClosed = meta.getOrDefault("dateClosed", null);

        List<String> categories = parseList(meta.getOrDefault("categories", ""));

        // optional: extract first heading or full body as description
        String description = extractDescription(body);

        return new Ticket.TicketBuilder()
                .setTicketId(UUID.randomUUID().toString())
                .setTitle(title)
                .setDescription(description)
                .setStatus(status)
                .setPrUrl(prUrl)
                .setClaimedBy(claimedBy)
                .setClosedBy(closedBy)
                .setPriority(priority)
                .setCategories(categories)
                .setDateAdded(dateAdded)
                .setDateClosed(dateClosed)
                .build();
    }

    private Map<String, String> parseMeta(String metaBlock) {
        Map<String, String> map = new HashMap<>();

        for (String line : metaBlock.split("\n")) {
            String[] kv = line.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }

        return map;
    }

    private List<String> parseList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String extractDescription(String body) {
        // simple version: take everything under first section
        return body.trim();
    }
}