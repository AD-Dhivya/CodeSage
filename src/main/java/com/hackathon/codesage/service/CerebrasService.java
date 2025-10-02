package com.hackathon.codesage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.codesage.config.CerebrasConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CerebrasService.class);

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
        if (fileName == null) return "java";

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
            case "rb" -> "ruby";
            case "php" -> "php";
            case "rs" -> "rust";
            default -> "java";
        };
    }

    /**
     * Multi-pass comprehensive code analysis
     * Each pass focuses on a specific aspect for deeper, more actionable insights
     */
    public String analyzeCode(String code, String language, String fileName) {
        logger.info("Starting comprehensive analysis for file: {} (language: {})", fileName, language);

        // Validation
        validateInputs(code, language);

        StringBuilder fullAnalysis = new StringBuilder();
        fullAnalysis.append("# 🔬 CodeSage Comprehensive Analysis\n");
        fullAnalysis.append(String.format("**File:** `%s` | **Language:** %s\n\n", fileName, language));
        fullAnalysis.append("---\n\n");

        try {
            // Pass 1: Security Analysis (CRITICAL - can block commits)
            logger.info("Running security analysis pass...");
            String securityAnalysis = analyzeWithPrompt(getSecurityPrompt(language, code));
            fullAnalysis.append("## 🛡️ Security Analysis\n\n");
            fullAnalysis.append(securityAnalysis).append("\n\n");
            fullAnalysis.append("---\n\n");

            // Pass 2: Design & Architecture
            logger.info("Running design analysis pass...");
            String designAnalysis = analyzeWithPrompt(getDesignPrompt(language, code));
            fullAnalysis.append("## 🏗️ Design & Architecture\n\n");
            fullAnalysis.append(designAnalysis).append("\n\n");
            fullAnalysis.append("---\n\n");

            // Pass 3: Performance Analysis
            logger.info("Running performance analysis pass...");
            String performanceAnalysis = analyzeWithPrompt(getPerformancePrompt(language, code));
            fullAnalysis.append("## ⚡ Performance Insights\n\n");
            fullAnalysis.append(performanceAnalysis).append("\n\n");
            fullAnalysis.append("---\n\n");

            // Pass 4: Readability & Maintainability
            logger.info("Running readability analysis pass...");
            String readabilityAnalysis = analyzeWithPrompt(getReadabilityPrompt(language, code));
            fullAnalysis.append("## 📖 Readability & Maintainability\n\n");
            fullAnalysis.append(readabilityAnalysis).append("\n\n");
            fullAnalysis.append("---\n\n");

            // Generate prioritized action items
            fullAnalysis.append("## 🎯 Prioritized Action Plan\n\n");
            fullAnalysis.append(generateActionItems(securityAnalysis, designAnalysis,
                    performanceAnalysis, readabilityAnalysis));

            logger.info("Analysis completed successfully");
            return fullAnalysis.toString();

        } catch (Exception e) {
            logger.error("Analysis failed for file: {}", fileName, e);
            return "❌ Analysis failed: " + e.getMessage();
        }
    }

    /**
     * Security-focused analysis prompt
     * Identifies vulnerabilities that could lead to real-world exploits
     */
    private String getSecurityPrompt(String language, String code) {
        return String.format("""
            You are a security architect specializing in %s security. Perform a SECURITY-ONLY analysis.
            
            CRITICAL SECURITY CHECKS (These BLOCK commits):
            1. Hardcoded credentials (passwords, API keys, tokens, secrets)
            2. SQL injection vulnerabilities (unsafe query construction)
            3. Command injection risks (unsafe system calls)
            4. Path traversal vulnerabilities (unsafe file operations)
            5. Insecure deserialization
            6. Missing authentication/authorization checks
            7. Cryptographic weaknesses (weak algorithms, hardcoded keys)
            8. XXE (XML External Entity) vulnerabilities
            
            HIGH PRIORITY SECURITY ISSUES (Educational feedback):
            - Missing input validation
            - Improper error handling exposing sensitive info
            - Insecure random number generation
            - Race conditions in security-critical code
            
            For EACH security issue found, respond EXACTLY like this:
            
            🚨 SECURITY VULNERABILITY DETECTED
            **Type:** [Specific vulnerability name - e.g., "Hardcoded API Credentials"]
            **Severity:** [CRITICAL or HIGH]
            **Location:** [Line numbers or specific code snippet]
            **Category:** Security
            
            🎯 **REAL-WORLD ATTACK SCENARIO:**
            [Describe concrete attack: "An attacker with access to [X] could [Y], leading to [Z]"]
            [Include real CVE or breach example if applicable]
            
            🛡️ **SECURE SOLUTION:**
            ```%s
            [Show EXACT secure code replacement with inline comments]
            ```
            
            📖 **SECURITY RESOURCES:**
            - OWASP Reference: [Specific OWASP page]
            - %s-specific security guide: [Relevant link]
            
            If NO security issues found:
            ✅ **NO SECURITY VULNERABILITIES DETECTED**
            Great job following security best practices!
            
            Code to analyze:
            ```%s
            %s
            ```
            """, language, language, language, language, code);
    }

    /**
     * Design and architecture analysis prompt
     * Focuses on SOLID principles, design patterns, and code smells
     */
    private String getDesignPrompt(String language, String code) {
        return String.format("""
            You are a software architect specializing in %s design patterns. Perform DESIGN-ONLY analysis.
            
            ANALYZE FOR:
            1. SOLID Principles violations
               - Single Responsibility: Does class/function do too much?
               - Open/Closed: Is it extensible without modification?
               - Liskov Substitution: Can subclasses replace parents safely?
               - Interface Segregation: Are interfaces too fat?
               - Dependency Inversion: Depend on abstractions, not concretions?
            
            2. Code Smells
               - God Class (class doing too much)
               - Long Method (function too complex)
               - Feature Envy (method using another class's data)
               - Shotgun Surgery (one change affects many classes)
               - Duplicate Code (DRY violations)
            
            3. Missing Design Patterns
               - Could Strategy pattern help?
               - Would Factory pattern improve creation?
               - Is Builder pattern needed for complex objects?
               - Would Observer pattern decouple components?
            
            For EACH design issue, respond EXACTLY like this:
            
            🏗️ DESIGN OPPORTUNITY
            **Pattern/Principle:** [Specific SOLID principle or code smell]
            **Severity:** [HIGH/MEDIUM/LOW]
            **Category:** Design
            
            🤔 **WHY THIS MATTERS:**
            - Maintenance Cost: [How this affects future changes]
            - Testing Difficulty: [How this complicates testing]
            - Team Impact: [How this affects other developers]
            
            ✨ **REFACTORED APPROACH:**
            ```%s
            [Show improved design with clear structure]
            ```
            
            💡 **DESIGN PATTERN TO EXPLORE:**
            [Suggest specific pattern with brief explanation]
            [Link to pattern documentation for %s]
            
            If code follows good design practices:
            ✅ **EXCELLENT DESIGN OBSERVED**
            [Highlight what's done well]
            
            Code to analyze:
            ```%s
            %s
            ```
            """, language, language, language, language, code);
    }

    /**
     * Performance analysis prompt
     * Identifies algorithmic inefficiencies and resource usage issues
     */
    private String getPerformancePrompt(String language, String code) {
        return String.format("""
            You are a performance engineer specializing in %s optimization. Perform PERFORMANCE-ONLY analysis.
            
            CHECK FOR:
            1. Algorithm Complexity
               - O(n²) or worse where O(n log n) or O(n) exists
               - Nested loops that could be flattened
               - Recursive calls without memoization
            
            2. Database/IO Issues
               - N+1 query problems
               - Missing batch operations
               - Unnecessary database calls in loops
               - Blocking I/O in async contexts
            
            3. Memory Issues
               - Memory leaks (unclosed resources)
               - Excessive object creation
               - Large collections loaded into memory
               - String concatenation in loops
            
            4. Caching Opportunities
               - Repeated expensive calculations
               - Cacheable external API calls
               - Static data fetched repeatedly
            
            For EACH performance issue, respond EXACTLY like this:
            
            ⚡ PERFORMANCE OPPORTUNITY
            **Issue:** [Specific bottleneck]
            **Severity:** [HIGH/MEDIUM/LOW]
            **Category:** Performance
            
            📊 **THE NUMBERS:**
            - Current Complexity: O([complexity]) - [What this means in plain English]
            - Optimized Complexity: O([complexity]) - [Expected improvement]
            - Real-world Impact: [e.g., "100ms vs 10ms for 1000 items"]
            
            🚄 **OPTIMIZED SOLUTION:**
            ```%s
            [Show faster algorithm/approach with performance comments]
            ```
            
            🔬 **HOW TO MEASURE:**
            [Suggest specific profiling technique for %s]
            
            If performance is good:
            ✅ **EFFICIENT IMPLEMENTATION**
            [Highlight what's optimized well]
            
            Code to analyze:
            ```%s
            %s
            ```
            """, language, language, language, language, code);
    }

    /**
     * Readability and maintainability analysis prompt
     * Focuses on clean code principles and developer experience
     */
    private String getReadabilityPrompt(String language, String code) {
        return String.format("""
            You are a clean code expert specializing in %s. Perform READABILITY-ONLY analysis.
            
            EVALUATE:
            1. Naming Conventions
               - Are names intention-revealing?
               - Do they follow %s conventions?
               - Are abbreviations avoided?
               - Do boolean variables sound like questions?
            
            2. Function Quality
               - Is each function doing ONE thing?
               - Are functions short (< 20 lines ideal)?
               - Do they have clear inputs/outputs?
               - Are side effects minimized?
            
            3. Code Comments
               - Do comments explain "WHY" not "WHAT"?
               - Are complex algorithms documented?
               - Are TODOs tracked?
               - Is there outdated/misleading documentation?
            
            4. Code Structure
               - Is indentation consistent?
               - Are there magic numbers/strings?
               - Is code DRY (Don't Repeat Yourself)?
               - Are error messages helpful?
            
            For EACH readability issue, respond EXACTLY like this:
            
            📖 READABILITY INSIGHT
            **Issue:** [Specific clarity problem]
            **Severity:** [MEDIUM/LOW]
            **Category:** Readability
            
            🧠 **COGNITIVE LOAD:**
            - Current: [How difficult code is to understand now]
            - Improved: [How much clearer it could be]
            
            ✍️ **CLEANER VERSION:**
            ```%s
            [Show refactored code with better names/structure/comments]
            ```
            
            💬 **CLEAN CODE PRINCIPLE:**
            [Share specific Uncle Bob or language-specific best practice]
            
            If readability is excellent:
            ✅ **HIGHLY READABLE CODE**
            [Highlight what makes it clear]
            
            Code to analyze:
            ```%s
            %s
            ```
            """, language, language, language, language, code);
    }

    /**
     * Execute a single analysis pass with the given prompt
     */
    private String analyzeWithPrompt(String prompt) {
        try {
            String requestBody = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are a professional code analysis expert. Provide specific, actionable insights. Always use the exact format requested."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "max_tokens": 1000,
                  "temperature": 0.2
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
                logger.error("API returned status {}: {}", response.statusCode(), response.body());
                return "⚠️ Analysis pass incomplete (API status: " + response.statusCode() + ")";
            }

        } catch (Exception e) {
            logger.error("Analysis pass failed", e);
            return "⚠️ Analysis pass failed: " + e.getMessage();
        }
    }

    /**
     * Generate prioritized action items from all analysis passes
     */
    private String generateActionItems(String security, String design, String performance, String readability) {
        StringBuilder items = new StringBuilder();

        // Critical Security Issues (BLOCKS COMMIT)
        if (security.contains("Severity: CRITICAL") || security.contains("Severity:** CRITICAL")) {
            items.append("### 🚨 CRITICAL - FIX IMMEDIATELY (BLOCKS COMMIT)\n\n");
            items.append("- **Security vulnerabilities detected** - Review security section above\n");
            items.append("- These must be resolved before commit can proceed\n\n");
        }

        // High Priority Issues
        boolean hasHighPriority = security.contains("Severity: HIGH") ||
                design.contains("Severity: HIGH") ||
                performance.contains("Severity: HIGH");

        if (hasHighPriority) {
            items.append("### ⚠️ HIGH PRIORITY - Address Soon\n\n");
            if (security.contains("Severity: HIGH")) {
                items.append("- **Security:** Review and fix high-priority security issues\n");
            }
            if (design.contains("Severity: HIGH")) {
                items.append("- **Design:** Refactor to address design concerns\n");
            }
            if (performance.contains("Severity: HIGH")) {
                items.append("- **Performance:** Optimize identified bottlenecks\n");
            }
            items.append("\n");
        }

        // Medium/Low Priority (Learning Opportunities)
        items.append("### 📚 LEARNING OPPORTUNITIES\n\n");
        items.append("1. Review suggested design patterns and best practices\n");
        items.append("2. Explore performance optimization techniques\n");
        items.append("3. Apply clean code principles for better readability\n");
        items.append("4. Check out the linked resources for deeper understanding\n\n");

        // Positive Reinforcement
        if (security.contains("NO SECURITY VULNERABILITIES") ||
                security.contains("EXCELLENT") ||
                design.contains("EXCELLENT") ||
                performance.contains("EFFICIENT") ||
                readability.contains("HIGHLY READABLE")) {
            items.append("### 🌟 STRENGTHS IDENTIFIED\n\n");
            items.append("Your code demonstrates solid practices in several areas!\n");
            items.append("Keep up the good work and continue learning.\n");
        }

        return items.toString();
    }

    /**
     * Validate inputs before processing
     */
    private void validateInputs(String code, String language) {
        Set<String> validLanguages = Set.of("java", "javascript", "python", "typescript",
                "go", "cpp", "c", "csharp", "ruby", "php", "rust");

        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("Language parameter is required");
        }

        if (!validLanguages.contains(language.toLowerCase())) {
            throw new IllegalArgumentException("Invalid programming language: " + language +
                    ". Supported: " + validLanguages);
        }

        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty or null");
        }

        if (code.length() > 50000) {
            throw new IllegalArgumentException("Code exceeds maximum size of 50KB");
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
            logger.error("Failed to parse API response", e);
            return "❌ Response parsing failed: " + e.getMessage();
        }
    }
}
