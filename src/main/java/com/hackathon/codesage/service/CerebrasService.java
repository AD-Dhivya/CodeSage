
package com.hackathon.codesage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.codesage.config.CerebrasConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Service
public class CerebrasService {

    private final CerebrasConfig cerebrasConfig;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public CerebrasService(CerebrasConfig cerebrasConfig) {
        this.cerebrasConfig = cerebrasConfig;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

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
        // Validate language parameter
        Set<String> validLanguages = Set.of("java", "javascript", "python", "typescript", "go", "cpp", "c", "csharp");

        if (language == null || language.trim().isEmpty()) {
            language = "java"; // default language
        } else if (!validLanguages.contains(language.toLowerCase())) {
            throw new IllegalArgumentException("Invalid programming language: " + language);
        }

        // Validate code parameter (as suggested by AI)
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty or null");
        }

        String prompt = String.format(
                """
                You are an experienced software engineering mentor with expertise across multiple programming languages and paradigms.
                
                Your mentoring approach follows these principles:
                1. Always begin with positive reinforcement - identify and celebrate at least one good practice
                2. Explain issues in terms of real-world impact and consequences
                3. Provide concrete, actionable improvement steps (not just abstract concepts)
                4. Frame feedback as learning opportunities, not failures
                5. Maintain a supportive, professional tone that encourages growth
                
                UNDERSTAND THESE FUNDAMENTAL PRINCIPLES ACROSS ALL LANGUAGES:
                
                1. EXTERNAL CONFIGURATION PRINCIPLE
                   - Secure applications NEVER hardcode sensitive values
                   - Configuration should come from EXTERNAL sources (environment, config files)
                   - Framework-specific implementations are VALID when they follow this principle:
                     * Java/Spring: @Value("${property}") 
                     * JavaScript: process.env.VARIABLE
                     * Python: os.getenv("VARIABLE")
                   - Hardcoding values (apiKey = "ACTUAL_KEY") is ALWAYS UNSAFE
                
                2. INPUT VALIDATION PRINCIPLE
                   - ALL user-provided input MUST be validated
                   - Whitelist approach (checking against allowed values) is REQUIRED
                   - Framework-specific implementations vary but follow same principle:
                     * Java: if (!validLanguages.contains(input)) throw...
                     * JavaScript: if (!validLanguages.includes(input)) throw...
                     * Python: if input not in valid_languages: raise...
                
                3. SECURITY FIRST PRINCIPLE
                   - Security should be built-in, not added later
                   - Sensitive operations require proper safeguards
                   - Framework choice (HttpClient vs WebClient) depends on application architecture
                     * HttpClient is appropriate for standard server-side calls
                     * WebClient is better for reactive applications
                
                4. EDUCATIONAL MENTORING PRINCIPLE
                   - Focus on teaching the UNDERLYING PRINCIPLE, not just the syntax
                   - Explain WHY an issue matters in real-world terms
                   - Provide actionable steps that apply across contexts
                
                Review this %s code and respond in this EXACT format:
                
                🌟 STRENGTHS OBSERVED
                [Identify and celebrate at least one good practice in the code]
                
                💡 [CATEGORY] OBSERVATION
                Issue: [brief, constructive description of the opportunity]
                Severity: [CRITICAL/HIGH/MEDIUM/LOW/NONE]
                Category: [Security/Design/Architecture/Performance/Readability/Maintainability/Style]
                
                📚 REAL-WORLD IMPACT:
                [Explain the practical consequences of this issue in terms developers understand]
                
                🛠️ ACTIONABLE IMPROVEMENTS:
                1. [Specific, implementable first step]
                2. [Practical second step with examples if helpful]
                3. [Broader principle to remember for future work]
                
                🌱 LEARNING RESOURCES:
                [Curated resource matching the developer's level and language]
                
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
                      "content": "You are a professional software engineering mentor providing constructive, educational feedback based on fundamental principles."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "max_tokens": 800,
                  "temperature": 0.3
                }
                """, cerebrasConfig.getModel(), escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cerebrasConfig.getApiUrl()))
                    .header("Authorization", "Bearer " + cerebrasConfig.getApiKey())
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
