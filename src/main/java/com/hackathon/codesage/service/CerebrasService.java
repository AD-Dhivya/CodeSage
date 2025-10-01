
package com.hackathon.codesage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class CerebrasService {

    @Value("${cerebras.api.key}")
    private String apiKey;

    @Value("${cerebras.api.url:https://api.cerebras.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${cerebras.model:llama3.1-8b}")
    private String model;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String detectLanguage(String fileName) {
        if (fileName == null) return "java"; // default to java now

        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (ext) {
            case "java" -> "java";
            case "js" -> "javascript";
            case "py" -> "python";
            case "ts" -> "typescript";
            case "go" -> "go";
            case "cpp", "h" -> "cpp";
            case "c" -> "c";
            case "cs" -> "csharp";
            default -> "java";
        };
    }

    public String analyzeCode(String code, String language, String fileName) {
        // Provide default values for optional parameters
        if (language == null || language.trim().isEmpty()) {
            language = "java"; // default language
        }

        String prompt = String.format(
                """
                You are a helpful code mentor. Review this %s code and respond in this EXACT format:
                
                🚨 SECURITY ISSUE DETECTED
                Issue: [brief description of the issue]
                Severity: [CRITICAL/HIGH/MEDIUM/LOW]
                Category: [Security/Performance/Bug/Style/etc.]
    
                🧑‍🏫 WHY THIS MATTERS:
                [Explain why this issue is important and what risks it poses]
    
                💡 HOW TO FIX:
                1. [First step to resolve the issue]
                2. [Second step]
                3. [Third step or more if needed]
    
                📚 LEARN MORE:
                [Provide a relevant link for further learning]
    
                Code to review:
                %s
                """,
                language,
                code
        );

        try {
            String requestBody = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are a helpful and educational code reviewer."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "max_tokens": 500,
                  "temperature": 0.3
                }
                """, model, escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                return "❌ API Error: " + response.statusCode() + "\n" + response.body();
            }

        } catch (Exception e) {
            return "❌ Request failed: " + e.getMessage();
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode contentNode = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content");

            if (contentNode.isMissingNode() || contentNode.asText().isEmpty()) {
                return "⚠️ AI returned empty analysis. Please check your input.";
            }
            return contentNode.asText();
        } catch (Exception e) {
            return "❌ Response parsing failed: " + e.getMessage();
        }
    }
}