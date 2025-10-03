
package com.hackathon.codesage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    private String summary;
    private String detailedAnalysis;
    private List<SecurityIssue> issues;
    private String status;
    private LocalDateTime timestamp;
    private String fileName;
    private String language;
}
