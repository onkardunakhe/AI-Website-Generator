package com.WebsiteDemoAgent.demoagent.controller;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.awt.Desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Component
public class Aitools {

    private final Path workspace =
            Path.of("generated-sites").toAbsolutePath().normalize();

    public Aitools() {
        try {
            Files.createDirectories(workspace);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create website workspace", e);
        }
    }

    @Tool(description = "Creates a new directory inside" +
            " the website workspace.")
    public String createDirectory(
            @ToolParam(description = "Relative directory " +
                    "path, for example brewlab") String path) {

        try {
            Path directory = safePath(path);
            Files.createDirectories(directory);
            return "Directory created successfully: " + path;
        } catch (IOException e) {
            return "Failed to create directory: " + e.getMessage();
        }
    }

    @Tool(description = """
            Creates or overwrites a text file inside the website workspace.
            Use this to create HTML, CSS and JavaScript files.
            """)
    public String writeFile(
            @ToolParam(description = "Relative file path, for example " +
                    "brewlab/index.html") String path,
            @ToolParam(description = "Complete content that should be " +
                    "written into the file") String content) {

        try {
            Path file = safePath(path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return "File written successfully: " + path;
        } catch (IOException e) {
            return "Failed to write file: " + e.getMessage();
        }
    }

    @Tool(
            description = "Reads the contents of an existing file from" +
                    " the website workspace.")
    public String readFile(
            @ToolParam(description = "Relative file path") String path) {

        try {
            return Files.readString(safePath(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Failed to read file: " + e.getMessage();
        }
    }

    @Tool(description = "Lists all files and directories inside" +
            " a website project.")
    public String listFiles(
            @ToolParam(description = "Relative directory path, " +
                    "for example brewlab") String path) {

        try {
            Path directory = safePath(path);

            if (!Files.exists(directory)) {
                return "Directory does not exist: " + path;
            }

            try (var files = Files.walk(directory)) {
                String result= files
                        .filter(file -> !file.equals(directory))
                        .map(workspace::relativize)
                        .map(Path::toString)
                        .collect(Collectors.joining("\n"));
                return result.isEmpty() ? "No files found in this directory." : result;
            }
        } catch (IOException e) {
            return "Failed to list files: " + e.getMessage();
        }
    }


    @Tool(description = """
        Opens the index.html file of a generated website
        in the default browser. Provide the project directory
        relative to the website workspace, for example brewlab.
        """)
    public String openWebsite(
            @ToolParam(description = "Website project directory name, "
                    + "for example brewlab")
            String projectPath) {

        try {
            System.out.println(">>> openWebsite called with: " + projectPath);

            Path projectDirectory = safePath(projectPath);
            Path indexFile = projectDirectory.resolve("index.html").normalize();

            System.out.println(">>> Resolved HTML path: " + indexFile);
            System.out.println(">>> File exists: " + Files.isRegularFile(indexFile));

            if (!Files.isRegularFile(indexFile)) {
                return "ERROR: index.html not found at: " + indexFile;
            }

            if (!Desktop.isDesktopSupported()) {
                return "ERROR: Java Desktop API is not supported.";
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.OPEN)
                    && !desktop.isSupported(Desktop.Action.BROWSE)) {
                return "ERROR: Neither OPEN nor BROWSE is supported.";
            }

            System.out.println(">>> Attempting to open browser...");

            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(indexFile.toUri());
            } else {
                desktop.open(indexFile.toFile());
            }

            System.out.println(">>> Browser opening request submitted.");

            return "Browser opening request submitted for: " + indexFile;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR opening website: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }


    private Path safePath(String path) {
        Path resolved = workspace.resolve(path).normalize();

        if (!resolved.startsWith(workspace)) {
            throw new IllegalArgumentException("Access outside generated-sites " +
                    "is not allowed");
        }

        return resolved;
    }
}
