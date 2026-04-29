package org.example.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TicketLoader {
    private final String rootDirectory;

    public TicketLoader(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public List<Path> getMarkdownFiles(String folderName) throws IOException {
        Path folderPath = Paths.get(rootDirectory, folderName);

        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IOException("Directory not found: " + folderPath.toString());
        }

        return Files.walk(folderPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".md"))
                .collect(Collectors.toList());
    }

    public String loadFileContent(Path filePath) throws IOException {
        return new String(Files.readAllBytes(filePath));
    }

    public String[] listTicketFolders() {
        java.io.File file = new java.io.File(rootDirectory);
        return file.list((current, name) -> new java.io.File(current, name).isDirectory());
    }
}
