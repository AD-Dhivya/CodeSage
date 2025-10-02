package com.hackathon.codesage.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CerebrasConfig {

    @Value("${cerebras.api.key}")
    private String apiKey;

    @Value("${cerebras.api.url:https://api.cerebras.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${cerebras.model:llama3.1-8b}")
    private String model;

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }
}