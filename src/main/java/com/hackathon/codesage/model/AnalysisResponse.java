package com.hackathon.codesage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    private String summary;
    private String detailedAnalysis;  // This will be your "feedback" content
    private List<CodeIssue> issues;
    private String status;
    private LocalDateTime timestamp;
    private String fileName;
    private String language;
    private Map<String, Object> staticAnalysis;
    private long responseTimeMs;
    private String poweredBy;  // ✅ ADDED for consistency
    private boolean success;   // ✅ ADDED for consistency

    // Helper method for success responses
    public static AnalysisResponse success(String summary, String detailedAnalysis,
                                           List<CodeIssue> issues, long responseTime) {
        return AnalysisResponse.builder()
                .summary(summary)
                .detailedAnalysis(detailedAnalysis)
                .issues(issues)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .responseTimeMs(responseTime)
                .poweredBy("Cerebras + Llama 3.1")
                .success(true)
                .build();
    }

    // Helper method for error responses
    public static AnalysisResponse error(String errorMessage) {
        return AnalysisResponse.builder()
                .summary("❌ Analysis failed")
                .detailedAnalysis("Error: " + errorMessage)
                .status("ERROR")
                .timestamp(LocalDateTime.now())
                .responseTimeMs(0)
                .poweredBy("Cerebras + Llama 3.1")
                .success(false)
                .build();
    }
}