package com.hackathon.codesage.controller;

import com.hackathon.codesage.model.AnalysisResponse;
import com.hackathon.codesage.service.CerebrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CodeAnalysisController {

    @Autowired
    private CerebrasService cerebrasService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeCode(@RequestBody Map<String, String> request) {
        long startTime = System.currentTimeMillis();

        try {
            String code = request.get("code");
            String fileName = request.get("fileName");
            String language = request.get("language");

            // Auto-detect language if not provided
            if (language == null && fileName != null) {
                language = detectLanguage(fileName);
            }
            if (language == null) language = "javascript";

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "error", "Code content is required",
                                "responseTimeMs", System.currentTimeMillis() - startTime
                        ));
            }

            // Call the service (returns AnalysisResponse now)
            AnalysisResponse analysisResult = cerebrasService.analyzeCode(code, language, fileName);
            long responseTime = System.currentTimeMillis() - startTime;

            // Convert AnalysisResponse to Map for backward compatibility
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "analysis", analysisResult.getDetailedAnalysis(),
                    "summary", analysisResult.getSummary(),
                    "issues", analysisResult.getIssues(),
                    "responseTimeMs", responseTime,
                    "status", analysisResult.getStatus(),
                    "poweredBy", "Cerebras + Llama 3.1"
            ));

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "responseTimeMs", responseTime
                    ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = cerebrasService.healthCheck();

            return ResponseEntity.ok(Map.of(
                    "status", isHealthy ? "healthy" : "unhealthy",
                    "service", "CodeSage API",
                    "cerebrasApi", isHealthy ? "connected" : "disconnected"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "unhealthy",
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Simple ping endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("🏓 pong - CodeSage is alive!");
    }

    /**
     * Detects programming language from file name
     */
    private String detectLanguage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }

        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.endsWith(".java")) return "java";
        if (lowerFileName.endsWith(".py")) return "python";
        if (lowerFileName.endsWith(".js")) return "javascript";
        if (lowerFileName.endsWith(".ts")) return "typescript";
        if (lowerFileName.endsWith(".jsx")) return "javascript";
        if (lowerFileName.endsWith(".tsx")) return "typescript";
        if (lowerFileName.endsWith(".go")) return "go";
        if (lowerFileName.endsWith(".rs")) return "rust";
        if (lowerFileName.endsWith(".c")) return "c";
        if (lowerFileName.endsWith(".cpp") || lowerFileName.endsWith(".cc")) return "cpp";
        if (lowerFileName.endsWith(".cs")) return "csharp";
        if (lowerFileName.endsWith(".php")) return "php";
        if (lowerFileName.endsWith(".rb")) return "ruby";
        if (lowerFileName.endsWith(".kt")) return "kotlin";
        if (lowerFileName.endsWith(".swift")) return "swift";
        if (lowerFileName.endsWith(".scala")) return "scala";

        return "javascript"; // default
    }
}
