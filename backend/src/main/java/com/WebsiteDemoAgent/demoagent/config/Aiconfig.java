package com.WebsiteDemoAgent.demoagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class Aiconfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder Builder){
        return Builder.defaultAdvisors(new SimpleLoggerAdvisor())
        .build();
    }
}
