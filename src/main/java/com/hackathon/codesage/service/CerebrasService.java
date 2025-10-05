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
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private DockerMCPService dockerMCPService;
    
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

    public AnalysisResponse analyzeCode(String code, String language, String fileName) {
        try {
            log.info("🔍 Starting enhanced code analysis for: {}", fileName);

            // Validate API key first
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("CEREBRAS_API_KEY not found in .env file");
            }

            // Step 1: Comprehensive Analysis (Security, Performance, Code Quality, Architecture, Clean Code)
            log.info("🔍 Running comprehensive code analysis...");
            List<CodeIssue> comprehensiveIssues = comprehensiveAnalysisService.analyzeCodeComprehensively(code, language, fileName);
            log.info("✅ Comprehensive analysis completed: {} issues found", comprehensiveIssues.size());

            // Step 2: Docker MCP Analysis
            log.info("🐳 Running Docker MCP analysis...");
            java.util.Map<String, Object> dockerResults = dockerMCPService.runContainerAnalysis(code, language, fileName);
            log.info("✅ Docker MCP analysis completed");

            // Step 3: Pattern Analysis
            String context = patternAnalyzer.analyzeContext(code);
            log.info("📊 Context analysis: {}", context);

            // Step 4: Enhanced context with all analysis results
            String enhancedContext = buildComprehensiveContext(context, dockerResults, comprehensiveIssues);
            log.info("📝 Enhanced context built: {} characters", enhancedContext.length());

            // Step 5: AI Analysis with enhanced context
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

            // Step 8: Build enhanced response
            return AnalysisResponse.builder()
                    .summary(extractComprehensiveSummary(analysisResult, allIssues))
                    .detailedAnalysis(analysisResult)
                    .issues(allIssues)
                    .staticAnalysis(dockerResults) // Docker MCP results
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
    private String buildComprehensiveContext(String patternContext, java.util.Map<String, Object> dockerResults, List<CodeIssue> comprehensiveIssues) {
        StringBuilder enhanced = new StringBuilder();
        
        enhanced.append("🔍 PATTERN ANALYSIS:\n");
        enhanced.append(patternContext).append("\n\n");
        
        enhanced.append("🐳 DOCKER MCP ANALYSIS:\n");
        enhanced.append("Container: ").append(dockerResults.get("containerName")).append("\n");
        enhanced.append("Language: ").append(dockerResults.get("language")).append("\n");
        enhanced.append("Docker MCP: ").append(dockerResults.get("dockerMCP")).append("\n");
        
        if (dockerResults.containsKey("containerAnalysis")) {
            enhanced.append("Container Analysis: ").append(dockerResults.get("containerAnalysis")).append("\n");
        }
        
        enhanced.append("\n📊 COMPREHENSIVE ANALYSIS RESULTS:\n");
        enhanced.append("Total Issues Found: ").append(comprehensiveIssues.size()).append("\n");
        
        // Group issues by category
        java.util.Map<String, List<CodeIssue>> issuesByCategory = comprehensiveIssues.stream()
                .collect(java.util.stream.Collectors.groupingBy(CodeIssue::getCategory));
        
        for (java.util.Map.Entry<String, List<CodeIssue>> entry : issuesByCategory.entrySet()) {
            enhanced.append("\n").append(entry.getKey()).append(" Issues: ").append(entry.getValue().size()).append("\n");
            for (CodeIssue issue : entry.getValue()) {
                enhanced.append("- ").append(issue.getType()).append(" (").append(issue.getSeverity()).append(")\n");
            }
        }
        
        return enhanced.toString();
    }
    
    /**
     * Extract comprehensive summary from analysis results
     */
    private String extractComprehensiveSummary(String analysis, List<CodeIssue> issues) {
        if (issues.isEmpty()) {
            return "✅ PASS: No issues detected - Great job!";
        }
        
        // Count issues by severity and category
        long criticalCount = issues.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity())).count();
        long highCount = issues.stream().filter(i -> "HIGH".equalsIgnoreCase(i.getSeverity())).count();
        long mediumCount = issues.stream().filter(i -> "MEDIUM".equalsIgnoreCase(i.getSeverity())).count();
        long lowCount = issues.stream().filter(i -> "LOW".equalsIgnoreCase(i.getSeverity())).count();
        
        // Count by category
        java.util.Map<String, Long> categoryCount = issues.stream()
                .collect(java.util.stream.Collectors.groupingBy(CodeIssue::getCategory, java.util.stream.Collectors.counting()));
        
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
        
        summary.append("\n📊 Issues by Category:\n");
        for (java.util.Map.Entry<String, Long> entry : categoryCount.entrySet()) {
            summary.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" issues\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Extract code issues from AI analysis
     */
    private List<CodeIssue> extractCodeIssues(String analysis) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // Pattern to match issue blocks
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

    private String callCerebrasApi(String prompt) throws Exception {
        // Validate API key
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
                                .content("You are a senior code mentor and application security expert.\n\nGoals:\n- Teach developers as you go (concise mentoring).\n- Identify security, correctness, performance, and readability issues.\n- Provide step-by-step fixes, with before/after code where helpful.\n- Propose refactoring and testing strategies.\n- Keep answers structured and actionable, optimized for skimming.\n\nConstraints:\n- Use the exact SECURITY section markers if a vulnerability is found so tools can parse.\n- Prefer minimal, incremental changes; highlight trade-offs.\n- Be definitive when possible, and note assumptions explicitly.")
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

        HttpResponse<String> httpResponse = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (httpResponse.statusCode() != 200) {
            log.error("❌ Cerebras API error: Status {}, Body: {}",
                    httpResponse.statusCode(),
                    httpResponse.body());
            throw new RuntimeException(
                    "Cerebras API error: " + httpResponse.statusCode() + " - " + httpResponse.body());
        }

        log.debug("📥 Response status: {}", httpResponse.statusCode());

        CerebrasResponse cerebrasResponse = objectMapper.readValue(
                httpResponse.body(),
                CerebrasResponse.class
        );

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

    private List<CodeIssue> extractSecurityIssues(String analysis) {
        List<CodeIssue> issues = new ArrayList<>();

        Pattern pattern = Pattern.compile("🚨 SECURITY VULNERABILITY DETECTED.*?(?=🚨|✅|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(analysis);

        while (matcher.find()) {
            String block = matcher.group();

            String type = extractField(block, "\\*\\*Type:\\*\\*\\s*(.+?)\\n");
            String severity = extractField(block, "\\*\\*Severity:\\*\\*\\s*(\\w+)");
            String location = extractField(block, "\\*\\*Location:\\*\\*\\s*(.+?)\\n");
            String description = extractField(block, "\\*\\*ATTACK SCENARIO:\\*\\*\\s*(.+?)(?=\\*\\*|$)");
            String recommendation = extractSecureFix(block);

            CodeIssue issue = CodeIssue.builder()
                    .type(type)
                    .severity(severity)
                    .location(location)
                    .description(description)
                    .recommendation(recommendation)
                    .category("Security")
                    .build();

            issues.add(issue);
            log.info("🔴 Extracted issue: {} ({})", issue.getType(), issue.getSeverity());
        }

        return issues;
    }

    private String extractField(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Not specified";
    }

    private String extractSecureFix(String block) {
        Pattern codeBlock = Pattern.compile("\\*\\*SECURE FIX:\\*\\*[\\s\\S]*?```[\\s\\S]*?```", Pattern.DOTALL);
        Matcher m = codeBlock.matcher(block);
        if (m.find()) {
            String section = m.group();
            Pattern inner = Pattern.compile("```[a-zA-Z0-9_-]*\\n([\\s\\S]*?)```", Pattern.DOTALL);
            Matcher innerM = inner.matcher(section);
            if (innerM.find()) {
                return innerM.group(1).trim();
            }
            return section.trim();
        }

        String fallback = extractField(block, "\\*\\*SECURE FIX:\\*\\*\\s*([\\s\\S]*?)(?=\\n\\*\\*|$)");
        return fallback;
    }

    private String extractSummary(String analysis, List<CodeIssue> issues) {
        if (issues.isEmpty()) {
            if (analysis.contains("✅ NO SECURITY VULNERABILITIES DETECTED")) {
                return "✅ PASS: No security vulnerabilities detected";
            } else {
                return "✅ Analysis completed - No critical issues found";
            }
        }

        long criticalCount = issues.stream()
                .filter(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity()))
                .count();
        long highCount = issues.stream()
                .filter(i -> "HIGH".equalsIgnoreCase(i.getSeverity()))
                .count();

        if (criticalCount > 0) {
            return String.format("🚨 CRITICAL: %d critical and %d high severity issues found", criticalCount, highCount);
        } else if (highCount > 0) {
            return String.format("⚠️ WARNING: %d high severity issues found", highCount);
        } else {
            return String.format("ℹ️ INFO: %d issues found", issues.size());
        }
    }

    public boolean healthCheck() {
        try {
            log.info("🏥 Performing Cerebras API health check...");
            String testPrompt = "Respond with 'OK' if you can read this message.";
            String response = callCerebrasApi(testPrompt);
            boolean isHealthy = response != null && !response.trim().isEmpty();
            log.info("🏥 Health check result: {}", isHealthy ? "✅ HEALTHY" : "❌ UNHEALTHY");
            return isHealthy;
        } catch (Exception e) {
            log.error("🏥 Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
