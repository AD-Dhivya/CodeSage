
package com.hackathon.codesage.controller;

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
                language = cerebrasService.detectLanguage(fileName);
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

            String analysisResult = cerebrasService.analyzeCode(code, language, fileName);
            long responseTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "analysis", analysisResult,
                    "responseTimeMs", responseTime,
                    "tokensUsed", 47, // placeholder for now
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
}