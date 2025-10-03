package com.hackathon.codesage.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PromptLoader {

    private static final String FEW_SHOT_FILE = "prompts/few-shot-examples.txt";
    private static final String ANALYSIS_TEMPLATE_FILE = "prompts/analysis-template.txt";

    private String fewShotExamples;
    private String analysisTemplate;

    @Value("${prompt.max.chars:6000}")
    private int maxPromptChars;

    @Value("${prompt.max.code.chars:3000}")
    private int maxCodeChars;

    @Value("${prompt.max.context.chars:800}")
    private int maxContextChars;

    @Value("${prompt.max.examples.chars:1200}")
    private int maxExamplesChars;

    /**
     * Loads the few-shot examples from the prompts directory
     */
    public String loadFewShotExamples() {
        if (fewShotExamples == null) {
            try {
                fewShotExamples = loadResourceFile(FEW_SHOT_FILE);
                log.info("✅ Loaded few-shot examples: {} characters", fewShotExamples.length());
            } catch (IOException e) {
                log.error("❌ Failed to load few-shot examples", e);
                fewShotExamples = ""; // Fallback to empty
            }
        }
        return fewShotExamples;
    }

    /**
     * Loads the analysis template from the prompts directory
     */
    public String loadAnalysisTemplate() {
        if (analysisTemplate == null) {
            try {
                analysisTemplate = loadResourceFile(ANALYSIS_TEMPLATE_FILE);
                log.info("✅ Loaded analysis template: {} characters", analysisTemplate.length());
            } catch (IOException e) {
                log.error("❌ Failed to load analysis template", e);
                // Fallback to a basic template
                analysisTemplate = createFallbackTemplate();
            }
        }
        return analysisTemplate;
    }

    /**
     * Builds the complete prompt with variable substitution
     */
    public String buildPrompt(String code, String language, String context) {
        String template = loadAnalysisTemplate();
        String examples = compact(loadFewShotExamples(), maxExamplesChars, "few-shot examples");
        String compactedCode = compact(code, maxCodeChars, "code");
        String compactedContext = compact(context != null ? context : "No specific context detected", maxContextChars, "context");

        // Replace placeholders
        Map<String, String> replacements = new HashMap<>();
        replacements.put("{{few_shot_examples}}", examples);
        replacements.put("{{code}}", compactedCode);
        replacements.put("{{language}}", language);
        replacements.put("{{context}}", compactedContext);

        String prompt = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            prompt = prompt.replace(entry.getKey(), entry.getValue());
        }

        if (prompt.length() > maxPromptChars) {
            String original = prompt;
            prompt = original.substring(0, Math.max(0, maxPromptChars - 100)) + "\n...\n[truncated for brevity]";
            log.info("📝 Built prompt truncated from {} to {} characters", original.length(), prompt.length());
        } else {
            log.info("📝 Built prompt: {} characters", prompt.length());
        }
        return prompt;
    }

    /**
     * Helper method to load a file from resources
     */
    private String loadResourceFile(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String content = reader.lines()
                    .collect(Collectors.joining("\n"));

            log.debug("📄 Loaded resource: {} ({} chars)", resourcePath, content.length());
            return content;
        }
    }

    /**
     * Creates a fallback template if the file couldn't be loaded
     */
    private String createFallbackTemplate() {
        return """
                {{few_shot_examples}}
                
                ═══════════════════════════════════════════════════════════
                YOUR ANALYSIS & MENTORING TASK
                ═══════════════════════════════════════════════════════════
                
                You are analyzing {{language}} code.
                
                DETECTED CONTEXT:
                {{context}}
                
                CODE TO ANALYZE:
                ```{{language}}
                {{code}}
                ```
                
                ═══════════════════════════════════════════════════════════
                RESPONSE FORMAT (MENTOR-FIRST):
                ═══════════════════════════════════════════════════════════
                
                1) QUICK SUMMARY (what this code does, key risks/strengths)
                - One or two sentences.

                2) STEP-BY-STEP IMPROVEMENTS
                - Bullet points with rationale.
                - Show minimal fixes first, then deeper refactors.
                
                3) BEFORE / AFTER (optional when helpful)
                - Before:
                ```{{language}}
                [problematic snippet]
                ```
                - After:
                ```{{language}}
                [improved snippet]
                ```

                4) TESTING GUIDANCE
                - Unit tests or integration tests to validate fixes.
                - Edge cases to cover.

                5) PERFORMANCE & READABILITY NOTES (optional)
                - Call out trade-offs clearly.

                If you find a CRITICAL security vulnerability:
                
                🚨 SECURITY VULNERABILITY DETECTED
                
                **Type:** [Specific vulnerability name]
                **Severity:** CRITICAL
                **Location:** [Show the exact problematic line]
                **Category:** Security
                
                **ATTACK SCENARIO:**
                [Explain how an attacker could exploit this]
                
                **SECURE FIX:**
                ```{{language}}
                [Show the corrected code]
                ```
                
                **LEARN MORE:**
                - OWASP: [Relevant link]
                
                ---
                
                If the code is secure:
                
                ✅ NO SECURITY VULNERABILITIES DETECTED
                
                **SECURITY STRENGTHS OBSERVED:**
                - [List security best practices found]
                
                **CODE QUALITY OBSERVATIONS:**
                - [Optional: design patterns, performance, readability]
                """;
    }

    /**
     * Reloads all templates (useful for development)
     */
    public void reloadTemplates() {
        fewShotExamples = null;
        analysisTemplate = null;
        log.info("🔄 Templates cleared, will reload on next use");
    }

    private String compact(String text, int maxChars, String label) {
        if (text == null) {
            return "";
        }
        if (maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        int cutoff = Math.max(0, maxChars - 80);
        String truncated = text.substring(0, cutoff) + "\n...\n[truncated " + label + " for brevity]";
        log.debug("✂️ Compacted {} from {} to {} chars", label, text.length(), truncated.length());
        return truncated;
    }
}