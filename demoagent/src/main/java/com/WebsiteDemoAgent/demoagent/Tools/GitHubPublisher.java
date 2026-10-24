package com.WebsiteDemoAgent.demoagent.Tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class GitHubPublisher {

    private final Path workspace =
            Path.of("generated-sites").toAbsolutePath().normalize();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final String API = "https://api.github.com";

    private static final int MAX_FILES = 100;
    private static final long MAX_FILE_BYTES = 1_000_000;

    private static final Pattern VALID_REPO_NAME =
            Pattern.compile("[a-zA-Z0-9._-]{1,100}");

    @Tool(description = """
            Publishes a completed static website to a NEW PUBLIC GitHub
            repository and configures GitHub Pages. Use this tool only
            when the user's request explicitly asks to publish, deploy,
            or upload the website to GitHub or GitHub Pages.
            First validate the website project and its root index.html.
            Parameters: projectPath is relative to generated-sites,
            repositoryName is the desired GitHub repository name,
            description is a short repository description.
            Returns the repository and Pages URLs, or an error.
            """)
    public String publishWebsiteToGitHubPages(
            @ToolParam(description =
                    "Website directory relative to generated-sites, "
                            + "for example freshmart")
            String projectPath,

            @ToolParam(description =
                    "Desired public GitHub repository name, "
                            + "for example freshmart-website")
            String repositoryName,

            @ToolParam(description =
                    "Short description of the website")
            String description) {

        try {
            return publish(projectPath, repositoryName, description);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Publishing was interrupted. Please retry.";
        } catch (Exception e) {
            return "GitHub publishing failed: " + e.getMessage();
        }
    }

    private String publish(
            String projectPath,
            String requestedRepositoryName,
            String description)
            throws IOException, InterruptedException {

        String token = System.getenv("GITHUB_TOKEN");

        if (token == null || token.isBlank()) {
            return "GITHUB_TOKEN is not configured. Set the environment "
                    + "variable and restart your application.";
        }

        Path project = safeProjectPath(projectPath);

        if (!Files.isDirectory(project)
                || Files.isSymbolicLink(project)) {
            return "Website project directory does not exist: "
                    + projectPath;
        }

        Path index = project.resolve("index.html");

        if (!Files.isRegularFile(index)
                || Files.isSymbolicLink(index)) {
            return "The project must contain a root index.html file.";
        }

        List<Path> files = collectWebsiteFiles(project);

        if (files.isEmpty() || files.size() > MAX_FILES) {
            return "The website must contain between 1 and "
                    + MAX_FILES + " publishable files.";
        }

        if (files.stream().noneMatch(
                file -> file.equals(project.resolve("index.html")))) {
            return "index.html was excluded from the publishable files.";
        }

        String repoName = normalizeRepositoryName(
                requestedRepositoryName);

        // Authenticate and discover the account that owns the new repo.
        JsonNode user = sendJson(
                "GET", "/user", null, token, 200);

        String owner = user.path("login").asText();

        if (owner.isBlank()) {
            throw new IllegalStateException(
                    "Could not determine the authenticated GitHub user.");
        }

        // Create a public repository with an initial commit.
        Map<String, Object> repoBody = new HashMap<>();
        repoBody.put("name", repoName);
        repoBody.put("description",
                description == null ? "" : description);
        repoBody.put("private", false);
        repoBody.put("auto_init", true);

        JsonNode repository = sendJson(
                "POST", "/user/repos", repoBody, token, 201);

        String branch = repository.path("default_branch")
                .asText("main");

        String repositoryPath = "/repos/"
                + encodeSegment(owner) + "/"
                + encodeSegment(repoName);

        // Upload each file as a GitHub Contents API commit.
        for (Path file : files) {
            Path relative = project.relativize(file);
            String repoFilePath = relative.toString()
                    .replace('\\', '/');

            byte[] bytes = Files.readAllBytes(file);

            Map<String, Object> fileBody = new HashMap<>();
            fileBody.put("message",
                    "Add website file: " + repoFilePath);
            fileBody.put("content",
                    Base64.getEncoder().encodeToString(bytes));
            fileBody.put("branch", branch);

            sendJson(
                    "PUT",
                    repositoryPath + "/contents/"
                            + encodePath(repoFilePath),
                    fileBody,
                    token,
                    201);
        }

        // Configure GitHub Pages to serve the root of the branch.
        Map<String, Object> pagesBody = Map.of(
                "build_type", "legacy",
                "source", Map.of(
                        "branch", branch,
                        "path", "/"));

        HttpResponse<String> pagesResponse = send(
                "POST",
                repositoryPath + "/pages",
                pagesBody,
                token);

        if (pagesResponse.statusCode() == 409) {
            // Pages may already be configured; update its source.
            send("PUT",
                    repositoryPath + "/pages",
                    pagesBody,
                    token);
        } else if (pagesResponse.statusCode() != 201) {
            throw apiError(pagesResponse);
        }

        // Fetch the authoritative published-site URL.
        JsonNode pages = sendJson(
                "GET",
                repositoryPath + "/pages",
                null,
                token,
                200);

        String siteUrl = pages.path("html_url").asText("");

        if (siteUrl.isBlank()) {
            siteUrl = "https://" + owner.toLowerCase(Locale.ROOT)
                    + ".github.io/" + repoName + "/";
        }

        return "Website files uploaded and GitHub Pages configured.\n"
                + "Repository: https://github.com/"
                + owner + "/" + repoName + "\n"
                + "Website URL: " + siteUrl + "\n"
                + "Publishing may take a few minutes to complete.";
    }

    private Path safeProjectPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Project directory is required.");
        }

        Path project = workspace.resolve(projectPath).normalize();

        if (!project.startsWith(workspace)
                || project.equals(workspace)) {
            throw new IllegalArgumentException(
                    "Project path must stay inside generated-sites.");
        }

        return project;
    }

    private List<Path> collectWebsiteFiles(Path project)
            throws IOException {

        List<Path> result = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(project)) {
            for (Path file : stream.toList()) {
                if (file.equals(project) || Files.isDirectory(file)) {
                    continue;
                }

                if (Files.isSymbolicLink(file)) {
                    continue;
                }

                if (!Files.isRegularFile(file)
                        || isExcluded(project.relativize(file))) {
                    continue;
                }

                long size = Files.size(file);

                if (size > MAX_FILE_BYTES) {
                    throw new IllegalArgumentException(
                            "File exceeds the 1 MB limit: "
                                    + project.relativize(file)
                                    + ". Optimize the asset and retry.");
                }

                result.add(file);
            }
        }

        return result;
    }

    private boolean isExcluded(Path relative) {
        for (Path part : relative) {
            String name = part.toString().toLowerCase(Locale.ROOT);

            if (name.startsWith(".")) {
                return true;
            }

            if (name.equals("node_modules")
                    || name.equals("target")
                    || name.equals("build")
                    || name.equals("dist")) {
                return true;
            }
        }

        String name = relative.getFileName().toString()
                .toLowerCase(Locale.ROOT);

        return name.equals("application.properties")
                || name.equals("application.yml")
                || name.equals("application.yaml")
                || name.endsWith(".log")
                || name.endsWith(".pem")
                || name.endsWith(".key")
                || name.endsWith(".p12")
                || name.endsWith(".jks")
                || name.equals("credentials.json")
                || name.startsWith(".env");
    }

    private String normalizeRepositoryName(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new IllegalArgumentException(
                    "Repository name is required.");
        }

        String name = requested.trim()
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("^[.-]+|[.-]+$", "");

        if (!VALID_REPO_NAME.matcher(name).matches()
                || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException(
                    "Invalid GitHub repository name: " + requested);
        }

        return name;
    }

    private JsonNode sendJson(
            String method,
            String endpoint,
            Object body,
            String token,
            int expectedStatus)
            throws IOException, InterruptedException {

        HttpResponse<String> response =
                send(method, endpoint, body, token);

        if (response.statusCode() != expectedStatus) {
            throw apiError(response);
        }

        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }

        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> send(
            String method,
            String endpoint,
            Object body,
            String token)
            throws IOException, InterruptedException {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(API + endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28");

        if (body == null) {
            builder.method(method,
                    HttpRequest.BodyPublishers.noBody());
        } else {
            String json = objectMapper.writeValueAsString(body);

            builder.header("Content-Type", "application/json")
                    .method(method,
                            HttpRequest.BodyPublishers.ofString(json));
        }

        return httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private IllegalStateException apiError(
            HttpResponse<String> response) {

        String body = response.body() == null
                ? "" : response.body();

        if (body.length() > 800) {
            body = body.substring(0, 800);
        }

        return new IllegalStateException(
                "GitHub API returned HTTP "
                        + response.statusCode() + ": " + body);
    }

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String encodePath(String path) {
        return Stream.of(path.split("/"))
                .map(this::encodeSegment)
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow();
    }
}