package com.hackathon.codesage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApiKeyService {

    @Value("${CEREBRAS_API_KEY}")
    private String apiKey;

    public String getCerebrasApiKey() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            log.info("✅ API key loaded from .env file");
            return apiKey;
        }

        log.error("❌ CEREBRAS_API_KEY not found in .env file");
        return "";
    }

    public boolean isApiKeyAvailable() {
        boolean available = apiKey != null && !apiKey.trim().isEmpty();
        log.info("API Key Status: {}", available ? "AVAILABLE" : "MISSING");
        return available;
    }
}
