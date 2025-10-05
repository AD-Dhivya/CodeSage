
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
    private String detailedAnalysis;
    private List<CodeIssue> issues;
    private String status;
    private LocalDateTime timestamp;
    private String fileName;
    private String language;
    private Map<String, Object> staticAnalysis;
}
