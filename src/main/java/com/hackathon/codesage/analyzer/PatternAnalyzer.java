package com.hackathon.codesage.analyzer;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PatternAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PatternAnalyzer.class);

    // Safe patterns that should NOT be flagged as vulnerabilities
    private static final Pattern EXTERNAL_CONFIG_PATTERN =
            Pattern.compile("@Value\\(.*?\\)|getenv\\(|System\\.getProperty|config\\.");

    private static final Pattern HTTP_CLIENT_PATTERN =
            Pattern.compile("HttpClient|HttpRequest|RestTemplate");

    private static final Pattern JSON_HANDLING_PATTERN =
            Pattern.compile("ObjectMapper|JsonNode|@JsonProperty");

    private static final Pattern INPUT_VALIDATION_PATTERN =
            Pattern.compile("Set\\.contains\\(|allowedValues\\.contains\\(|whitelist");

    private static final Pattern PREPARED_STATEMENT_PATTERN =
            Pattern.compile("PreparedStatement|setString\\(|setInt\\(");

    private static final Pattern ESCAPE_FUNCTION_PATTERN =
            Pattern.compile("escapeHtml|escapeJson|escapeXml|sanitize");

    /**
     * Analyzes code and returns a context string describing safe patterns found
     */
    public String analyzeContext(String code) {
        List<String> detectedPatterns = new ArrayList<>();

        if (EXTERNAL_CONFIG_PATTERN.matcher(code).find()) {
            detectedPatterns.add("External configuration detected (@Value, getenv) - NOT hardcoded credentials");
        }

        if (HTTP_CLIENT_PATTERN.matcher(code).find()) {
            detectedPatterns.add("HTTP client usage detected - Standard Java HTTP library, NOT SQL injection");
        }

        if (JSON_HANDLING_PATTERN.matcher(code).find()) {
            detectedPatterns.add("JSON handling detected - Standard Jackson library, NOT a vulnerability");
        }

        if (INPUT_VALIDATION_PATTERN.matcher(code).find()) {
            detectedPatterns.add("Input validation detected - Whitelist approach (SECURE)");
        }

        if (PREPARED_STATEMENT_PATTERN.matcher(code).find()) {
            detectedPatterns.add("PreparedStatement usage detected - SQL injection prevention (SECURE)");
        }

        if (ESCAPE_FUNCTION_PATTERN.matcher(code).find()) {
            detectedPatterns.add("Escape/sanitization functions detected - XSS prevention (SECURE)");
        }

        if (detectedPatterns.isEmpty()) {
            log.debug("No specific safe patterns detected in code");
            return "No specific context detected";
        }

        String context = String.join("\n- ", detectedPatterns);
        log.info("✅ Detected {} safe patterns in code", detectedPatterns.size());

        return "- " + context;
    }

    /**
     * Checks if code contains actual hardcoded credentials (VALUES visible)
     */
    public boolean hasHardcodedCredentials(String code) {
        // Look for actual credential VALUES, not just variable names
        Pattern hardcodedPattern = Pattern.compile(
                "(apiKey|password|secret|token)\\s*=\\s*[\"'][^\"']{10,}[\"']",
                Pattern.CASE_INSENSITIVE
        );

        return hardcodedPattern.matcher(code).find();
    }

    /**
     * Checks if code contains SQL concatenation (actual vulnerability)
     */
    public boolean hasSqlConcatenation(String code) {
        Pattern sqlConcatPattern = Pattern.compile(
                "\"SELECT.*?\"\\s*\\+|\"INSERT.*?\"\\s*\\+|\"UPDATE.*?\"\\s*\\+|\"DELETE.*?\"\\s*\\+"
        );

        return sqlConcatPattern.matcher(code).find();
    }

    /**
     * Checks if code contains command injection risk
     */
    public boolean hasCommandInjection(String code) {
        Pattern cmdPattern = Pattern.compile(
                "Runtime\\.getRuntime\\(\\)\\.exec\\(.*?\\+|ProcessBuilder\\(.*?\\+"
        );

        return cmdPattern.matcher(code).find();
    }

    /**
     * Provides a summary of detected vulnerabilities
     */
    public String getVulnerabilitySummary(String code) {
        List<String> vulnerabilities = new ArrayList<>();

        if (hasHardcodedCredentials(code)) {
            vulnerabilities.add("⚠️ Hardcoded credentials detected");
        }

        if (hasSqlConcatenation(code)) {
            vulnerabilities.add("⚠️ SQL concatenation detected");
        }

        if (hasCommandInjection(code)) {
            vulnerabilities.add("⚠️ Command injection risk detected");
        }

        if (vulnerabilities.isEmpty()) {
            return "✅ No obvious vulnerabilities detected by static analysis";
        }

        return String.join("\n", vulnerabilities);
    }
}