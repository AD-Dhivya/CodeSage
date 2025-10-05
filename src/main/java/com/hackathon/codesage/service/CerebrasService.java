
package com.hackathon.codesage.service;

import com.hackathon.codesage.analyzer.PatternAnalyzer;
import com.hackathon.codesage.model.AnalysisResponse;
import com.hackathon.codesage.model.CerebrasRequest;
import com.hackathon.codesage.model.CerebrasResponse;
import com.hackathon.codesage.model.CodeIssue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CerebrasService {

    private static final Logger log = LoggerFactory.getLogger(CerebrasService.class);

    @Value("${cerebras.api.url}")
    private String apiUrl;

    @Value("${cerebras.api.model}")
    private String model;

    @Value("${cerebras.api.max-tokens}")
    private int maxTokens;

    @Value("${cerebras.api.temperature}")
    private double temperature;

    @Value("${CEREBRAS_API_KEY}")
    private String apiKey;

    @Autowired
    private PromptLoader promptLoader;

    @Autowired
    private PatternAnalyzer patternAnalyzer;

    @Autowired
    private ComprehensiveAnalysisService comprehensiveAnalysisService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${http.request.timeout.ms:15000}")
    private int requestTimeoutMs;

    @Autowired
    public CerebrasService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        log.info("🔑 CerebrasService initialized - API key loaded from .env");
    }

    @Cacheable(value = "analysis", key = "#code.hashCode()")
    public AnalysisResponse analyzeCode(String code, String language, String fileName) {
        try {
            log.info("🔍 Starting enhanced code analysis for: {}", fileName);

            // Validate API key
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("CEREBRAS_API_KEY not found in .env file");
            }

            // Step 1: Comprehensive Analysis
            log.info("🔍 Running comprehensive code analysis...");
            List<CodeIssue> comprehensiveIssues = comprehensiveAnalysisService.analyzeCodeComprehensively(code, language, fileName);
            log.info("✅ Comprehensive analysis completed: {} issues found", comprehensiveIssues.size());

            // Step 2: Docker MCP Analysis → Not available in container
            log.info("⚠️ Docker MCP analysis skipped: Not supported in Docker environment");
            Map<String, Object> dockerResults = new HashMap<>();
            dockerResults.put("dockerMCP", false);
            dockerResults.put("containerAnalysis", "Not available in container");
            dockerResults.put("containerName", "N/A");
            dockerResults.put("language", language);
            dockerResults.put("reason", "Docker-in-Docker not enabled");

            // Step 3: Pattern Analysis
            String context = patternAnalyzer.analyzeContext(code);
            log.info("📊 Context analysis: {}", context);

            // Step 4: Build enhanced context
            String enhancedContext = buildComprehensiveContext(context, dockerResults, comprehensiveIssues);
            log.info("📝 Enhanced context built: {} characters", enhancedContext.length());

            // Step 5: Build prompt and call AI
            String fullPrompt = promptLoader.buildPrompt(code, language, enhancedContext);
            log.info("📝 Prompt built: {} characters", fullPrompt.length());

            String analysisResult = callCerebrasApi(fullPrompt);
            log.info("✅ Received AI analysis: {} characters", analysisResult.length());

            // Step 6: Extract AI-identified issues
            List<CodeIssue> aiIssues = extractCodeIssues(analysisResult);

            // Step 7: Combine all issues
            List<CodeIssue> allIssues = new ArrayList<>();
            allIssues.addAll(comprehensiveIssues);
            allIssues.addAll(aiIssues);

            // Step 8: Build final response
            return AnalysisResponse.builder()
                    .summary(extractComprehensiveSummary(analysisResult, allIssues))
                    .detailedAnalysis(analysisResult)
                    .issues(allIssues)
                    .staticAnalysis(dockerResults)
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .fileName(fileName)
                    .language(language)
                    .build();
        } catch (Exception e) {
            log.error("❌ Analysis failed for {}: {}", fileName, e.getMessage(), e);
            return AnalysisResponse.builder()
                    .summary("Analysis failed: " + e.getMessage())
                    .detailedAnalysis("Error: " + e.getMessage())
                    .issues(new ArrayList<>())
                    .status("ERROR")
                    .timestamp(LocalDateTime.now())
                    .fileName(fileName)
                    .language(language)
                    .build();
        }
    }

    /**
     * Build comprehensive context combining all analysis results
     */
    private String buildComprehensiveContext(String patternContext, Map<String, Object> dockerResults, List<CodeIssue> comprehensiveIssues) {
        StringBuilder enhanced = new StringBuilder();

        enhanced.append("🔍 PATTERN ANALYSIS:\n");
        enhanced.append(patternContext).append("\n\n");

        enhanced.append("🐳 DOCKER MCP ANALYSIS:\n");
        enhanced.append("Status: Not available in container\n");
        enhanced.append("Container: ").append(dockerResults.get("containerName")).append("\n");
        enhanced.append("Language: ").append(dockerResults.get("language")).append("\n");
        enhanced.append("Note: Docker MCP not supported in current container setup\n\n");

        enhanced.append("📊 COMPREHENSIVE ANALYSIS RESULTS:\n");
        enhanced.append("Total Issues Found: ").append(comprehensiveIssues.size()).append("\n");

        Map<String, List<CodeIssue>> issuesByCategory = comprehensiveIssues.stream()
                .collect(Collectors.groupingBy(CodeIssue::getCategory));

        for (Map.Entry<String, List<CodeIssue>> entry : issuesByCategory.entrySet()) {
            enhanced.append("\n").append(entry.getKey()).append(" Issues: ").append(entry.getValue().size()).append("\n");
            for (CodeIssue issue : entry.getValue()) {
                enhanced.append("- ").append(issue.getType()).append(" (").append(issue.getSeverity()).append(")\n");
            }
        }

        return enhanced.toString();
    }

    /**
     * Extract comprehensive summary from AI analysis and issue list
     */
    private String extractComprehensiveSummary(String analysis, List<CodeIssue> issues) {
        if (issues.isEmpty()) {
            return "✅ PASS: No issues detected - Great job!";
        }

        long criticalCount = issues.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity())).count();
        long highCount = issues.stream().filter(i -> "HIGH".equalsIgnoreCase(i.getSeverity())).count();
        long mediumCount = issues.stream().filter(i -> "MEDIUM".equalsIgnoreCase(i.getSeverity())).count();
        long lowCount = issues.stream().filter(i -> "LOW".equalsIgnoreCase(i.getSeverity())).count();

        StringBuilder summary = new StringBuilder();

        if (criticalCount > 0) {
            summary.append("🚨 CRITICAL: ").append(criticalCount).append(" critical issues found");
        } else if (highCount > 0) {
            summary.append("⚠️ HIGH: ").append(highCount).append(" high priority issues found");
        } else if (mediumCount > 0) {
            summary.append("ℹ️ MEDIUM: ").append(mediumCount).append(" medium priority issues found");
        } else {
            summary.append("💡 LOW: ").append(lowCount).append(" low priority issues found");
        }

        Map<String, Long> categoryCount = issues.stream()
                .collect(Collectors.groupingBy(CodeIssue::getCategory, Collectors.counting()));

        summary.append("\n📊 Issues by Category:\n");
        for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            summary.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" issues\n");
        }

        return summary.toString();
    }

    /**
     * Extract code issues from AI analysis using regex
     */
    private List<CodeIssue> extractCodeIssues(String analysis) {
        List<CodeIssue> issues = new ArrayList<>();

        Pattern pattern = Pattern.compile("🚨.*?(?=🚨|✅|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(analysis);

        while (matcher.find()) {
            String block = matcher.group();

            String type = extractField(block, "\\*\\*Type:\\*\\*\\s*(.+?)\\n");
            String severity = extractField(block, "\\*\\*Severity:\\*\\*\\s*(\\w+)");
            String location = extractField(block, "\\*\\*Location:\\*\\*\\s*(.+?)\\n");
            String description = extractField(block, "\\*\\*Description:\\*\\*\\s*(.+?)(?=\\*\\*|$)");
            String recommendation = extractField(block, "\\*\\*Recommendation:\\*\\*\\s*(.+?)(?=\\*\\*|$)");
            String category = extractField(block, "\\*\\*Category:\\*\\*\\s*(.+?)\\n");

            CodeIssue issue = CodeIssue.builder()
                    .type(type)
                    .severity(severity)
                    .location(location)
                    .description(description)
                    .recommendation(recommendation)
                    .category(category)
                    .explanation("AI-identified issue")
                    .example("See code above")
                    .bestPractice("Follow the recommendation")
                    .learningResource("https://github.com/your-repo/CodeSage")
                    .build();

            issues.add(issue);
        }

        return issues;
    }

    /**
     * Call Cerebras API for AI-powered analysis
     */
    private String callCerebrasApi(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("CEREBRAS_API_KEY is not set in .env file");
        }

        CerebrasRequest cerebrasRequest = CerebrasRequest.builder()
                .model(model)
                .temperature(temperature)
                .max_tokens(maxTokens)
                .messages(List.of(
                        CerebrasRequest.Message.builder()
                                .role("system")
                                .content("You are a senior code mentor and application security expert.\n\n" +
                                        "Goals:\n" +
                                        "- Teach developers as you go (concise mentoring).\n" +
                                        "- Identify security, correctness, performance, and readability issues.\n" +
                                        "- Provide step-by-step fixes, with before/after code where helpful.\n" +
                                        "- Propose refactoring and testing strategies.\n" +
                                        "- Keep answers structured and actionable, optimized for skimming.\n\n" +
                                        "Constraints:\n" +
                                        "- Use the exact SECURITY section markers if a vulnerability is found so tools can parse.\n" +
                                        "- Prefer minimal, incremental changes; highlight trade-offs.\n" +
                                        "- Be definitive when possible, and note assumptions explicitly.")
                                .build(),
                        CerebrasRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();

        String requestBody = objectMapper.writeValueAsString(cerebrasRequest);
        log.debug("📤 Request body: {}...", requestBody.substring(0, Math.min(200, requestBody.length())));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMillis(Math.max(1000, requestTimeoutMs)))
                .build();

        log.info("🌐 Calling Cerebras API: {}", apiUrl);

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            log.error("❌ Cerebras API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
            throw new RuntimeException("Cerebras API error: " + httpResponse.statusCode() + " - " + httpResponse.body());
        }

        CerebrasResponse cerebrasResponse = objectMapper.readValue(httpResponse.body(), CerebrasResponse.class);

        if (cerebrasResponse.getChoices() == null || cerebrasResponse.getChoices().isEmpty()) {
            throw new RuntimeException("Empty response from Cerebras API");
        }

        String content = cerebrasResponse.getChoices().get(0).getMessage().getContent();

        if (cerebrasResponse.getUsage() != null) {
            log.info("📊 Token usage - Prompt: {}, Completion: {}, Total: {}",
                    cerebrasResponse.getUsage().getPrompt_tokens(),
                    cerebrasResponse.getUsage().getCompletion_tokens(),
                    cerebrasResponse.getUsage().getTotal_tokens());
        }

        return content;
    }

    /**
     * Extract field using regex
     */
    private String extractField(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Not specified";
    }

    /**
     * Health check for Cerebras API
     */
    public boolean healthCheck() {
        try {
            log.info("🏥 Performing Cerebras API health check...");
            String testPrompt = "Respond with 'OK' if you can read this message.";
            String response = callCerebrasApi(testPrompt);
            boolean isHealthy = response != null && response.contains("OK");
            log.info("🏥 Health check result: {}", isHealthy ? "✅ HEALTHY" : "❌ UNHEALTHY");
            return isHealthy;
        } catch (Exception e) {
            log.error("🏥 Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
