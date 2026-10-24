package com.WebsiteDemoAgent.demoagent.controller;

import com.WebsiteDemoAgent.demoagent.Tools.GitHubPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1")
@CrossOrigin("*")
public class Chatcontroller {

    private final ChatClient chatClient;
    private final Aitools aitools;
    private final GitHubPublisher gitHubPublisher;

    List<Message> messages=new ArrayList<>();

    private static final String SYSTEM_PROMPT = """
            You are an expert frontend website developer.

            Your job is to create complete static websites using the available tools.

            Follow these rules:
            1. Create a separate directory for every website.
            2. Create index.html.
            3. Create style.css.
            4. Create script.js when JavaScript is useful.
            5. Build modern, beautiful and responsive websites.
            6. Use only HTML, CSS and vanilla JavaScript.
            7. Do not just return website code in your response. Actually create the files using tools.
            8. After creating the website, list the project files.
            9. Read important files again if needed and fix obvious problems.
            10. Finish only when the complete website has been created.
            
            
            GITHUB PUBLISHING RULES
            
            1. Do not publish a website merely because the user asks you to create one.
            
            2. Only invoke publishWebsiteToGitHubPages when the user's
               request explicitly asks to publish, deploy, or upload the
               website to GitHub or GitHub Pages.
            
            3. Before publishing, finish generating the website and verify
               that the root index.html exists.
            
            4. Pass the exact project directory relative to generated-sites.
            
            5. Choose a descriptive repository name using letters, numbers,
               hyphens, underscores, or dots.
            
            6. The publishing tool creates a NEW PUBLIC GitHub repository.
               Never claim a private repository was created.
            
            7. Report the repository URL and live website URL returned by
               the tool. Explain if GitHub Pages is still building.
            
            8. Never claim deployment succeeded if the tool returns an error.
            
            9. Never publish API keys, credentials, environment files, or
               unrelated files.
            
            10. If the user only requests a local website, generate it and
                open it locally as requested. Do not publish it.
            
            """;

    @PostMapping("/chat")
    public String chat(@RequestBody String query){
        System.out.println("Request Came");
        messages.add(new UserMessage(query));
        String response= chatClient
                .prompt(query)
                .system(SYSTEM_PROMPT)
                .messages(messages)
                .tools(aitools,gitHubPublisher)
                .call()
                .content();

        messages.add(new AssistantMessage(response));
        return response;
    }


}
